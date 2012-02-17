/**
	Copyright (C) 2009,2010  Tobias Domhan

    This file is part of AndOpenGLCam.

    AndObjViewer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    AndObjViewer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with AndObjViewer.  If not, see <http://www.gnu.org/licenses/>.
 
 */
package edu.dhbw.andar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.*;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.alien_roger.android.ackdev.webservices.ImageDownloader;
import com.alien_roger.android.ackdev.webservices.RI_WebService;
import com.alien_roger.android.ackdev.zxing.BarcodeFormat;
import com.alien_roger.android.ackdev.zxing.Result;
import com.alien_roger.android.ackdev.zxing.ResultMetadataType;
import com.alien_roger.android.ackdev.zxing.ResultPoint;
import com.alien_roger.android.ackdev.zxing.client.android.*;
import com.alien_roger.android.ackdev.zxing.client.android.result.ResultHandler;
import com.alien_roger.android.ackdev.zxing.client.android.result.ResultHandlerFactory;
import com.google.gson.Gson;
import com.javacodegeeks.android.json.model.SearchResponse;
import edu.dhbw.andar.exceptions.AndARException;
import edu.dhbw.andar.exceptions.AndARRuntimeException;
import edu.dhbw.andar.history.HistoryManager;
import edu.dhbw.andar.interfaces.OpenGLRenderer;
import edu.dhbw.andar.pub.CustomObject;
import edu.dhbw.andar.pub.CustomRenderer;
import edu.dhbw.andar.util.IO;
import edu.dhbw.andobjviewer.graphics.Model3D;
import edu.dhbw.andobjviewer.models.Model;
import edu.dhbw.andobjviewer.parser.ObjParser;
import edu.dhbw.andobjviewer.parser.ParseException;
import edu.dhbw.andobjviewer.parser.Util;
import edu.dhbw.andobjviewer.util.AssetsFileUtil;
import edu.dhbw.andobjviewer.util.BaseFileUtil;
import edu.dhbw.andobjviewer.util.SDCardFileUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

//public class AndARActivity extends Activity implements Callback/*, UncaughtExceptionHandler*/{
public class AndARActivity extends Activity implements  SurfaceHolder.Callback{

    private static final String TAG = AndARActivity.class.getSimpleName();
    private static final String TAG_INFO = AndARActivity.class.getSimpleName() + "_INFO";
    private static final String TAG_DEBUG = AndARActivity.class.getSimpleName() + "_DEBUG";
    private static final String TAG_ERROR = AndARActivity.class.getSimpleName() + "_ERROR";


    // TODO andar model addition
	private Model model;
	private Model3D model3d;
	/**
	 * View a file in the assets folder
	 */
	public static final int TYPE_INTERNAL = 0;
	/**
	 * View a file on the sd card.
	 */
	public static final int TYPE_EXTERNAL = 1;




    // default andar definnitions
	private GLSurfaceView glSurfaceView;
	private Camera camera;
	private AndARRenderer renderer;
	private Resources res;
	private CameraPreviewHandler cameraHandler;
	private boolean mPausing = false;
	private ARToolkit artoolkit;
	private final CameraStatus camStatus = new CameraStatus();
	private boolean surfaceCreated = false;
//    private Preview previewSurface;
//    private SurfaceView previewSurface;
	private boolean startPreviewRightAway;



    private SurfaceHolder mSurfaceHolder; // default andar SurfaceHodler for camera init
    public AndARActivity() {
		startPreviewRightAway = true;
	}
	
	public AndARActivity(boolean startPreviewRightAway) {
		this.startPreviewRightAway = startPreviewRightAway;
	}


    // ------------------------------------------------------
    // zxing injection


    private static final int SHARE_ID = Menu.FIRST;
    private static final int HISTORY_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int HELP_ID = Menu.FIRST + 3;
    private static final int ABOUT_ID = Menu.FIRST + 4;

    private static final long INTENT_RESULT_DURATION = 1500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private static final String PACKAGE_NAME = " edu.dhbw.andar";
    private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";
    private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";
    private static final String ZXING_URL = "http://zxing.appspot.com/scan";
    private static final String RETURN_CODE_PLACEHOLDER = "{CODE}";
    private static final String RETURN_URL_PARAM = "ret";

    private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;

    // Roger's
    private final ImageDownloader imageDownloader = new ImageDownloader();

//    public void uncaughtException(Thread thread, Throwable ex) {
//		Log.e("AndAR EXCEPTION", ex.getMessage());
//		finish();
//    }

    static {
        DISPLAYABLE_METADATA_TYPES = new HashSet<ResultMetadataType>(5);
        DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ISSUE_NUMBER);
        DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.SUGGESTED_PRICE);
        DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ERROR_CORRECTION_LEVEL);
        DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.POSSIBLE_COUNTRY);
    }

    private enum Source {
        NATIVE_APP_INTENT,
        PRODUCT_SEARCH_LINK,
        ZXING_LINK,
        NONE
    }

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private TextView statusView;
    private View resultView;
    private ImageView barcodeImageView;
    private Result lastResult;
    private boolean hasSurface;
    private boolean copyToClipboard;
    private Source source;
    private String sourceUrl;
    private String returnUrlTemplate;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private String versionName;
    private HistoryManager historyManager;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;


    // Roger's additions
    private WebView barcodeBrowser;
    private final RI_WebService riWebService = null;
    private SearchResponse searchResponse;



    // andar and zxing devider
    // set flag that will toggle camera's
    private final boolean isAndar = true;
    private boolean zxingCamMode = true;

    private CustomObject someObject;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Original andar part goes below
        // TODO must be called before adding features
        setFullscreen();
        disableScreenTurnOff();


        // load model immediate
        startPreviewRightAway = true;

        res = getResources();
        artoolkit = new ARToolkit(res, getFilesDir());

        setContentView(R.layout.capture);
        ZxingCameraManager.init(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);

        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);

        handler = null;
        lastResult = null;
        hasSurface = false;
        historyManager = new HistoryManager(this);
        historyManager.trimHistory();
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);


        //orientation is set via the manifest

        try {
            IO.transferFilesToPrivateFS(getFilesDir(),res);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AndARRuntimeException(e.getMessage());
        }

        renderer = new AndARRenderer(res, artoolkit, this);
        // TODO Roger's addition set renderer to get lightning on the object
        renderer.setNonARRenderer( new CustomRenderer());
//        renderer.setNonARRenderer( new LightingRenderer());

        glSurfaceView =(GLSurfaceView)findViewById(R.id.gl_surface_view);

        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


        cameraHandler = new CameraPreviewHandler(glSurfaceView, renderer, res, artoolkit, camStatus);


        if(Config.DEBUG)
            Debug.startMethodTracing("AndAR");

        // Loadd simple andar model object (cube)
        try {
            someObject = new CustomObject
            ("test", "patt.hiro", 80.0, new double[]{0,0});
            artoolkit.registerARObject(someObject);
            someObject = new CustomObject
            ("test", "android.patt", 80.0, new double[]{0,0});
            artoolkit.registerARObject(someObject);

        } catch (AndARException ex){
            //handle the exception, that means: show the user what happened
            System.out.println("");
        }



        // Roger's additional overlays view for image and html
        barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        barcodeBrowser = (WebView) findViewById(R.id.webkit);
        // JSON parser response class
        searchResponse = new SearchResponse();




        
//        setContentView(frame);
        if(Config.DEBUG)
        	Debug.startMethodTracing("AndAR");
    }
    



    @Override
    protected void onResume() {
        // TODO zxing inection

        // andar part
        mPausing = false;
        glSurfaceView.onResume();

        resetStatusView();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
//            SurfaceView surfaceView = new Preview(AndARActivity.this);

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }


        // zxing intents parse actions
        Intent intent = getIntent();
        String action = intent == null ? null : intent.getAction();
        String dataString = intent == null ? null : intent.getDataString();
        if (intent != null && action != null) {
            if (action.equals(Intents.Scan.ACTION)) {
                // Scan the formats the intent requested, and return the result to the calling activity.
                source = Source.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
            } else if (dataString != null && dataString.contains(PRODUCT_SEARCH_URL_PREFIX) &&
                    dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {
                // Scan only products and send the result to mobile Product Search.
                source = Source.PRODUCT_SEARCH_LINK;
                sourceUrl = dataString;
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;
            } else if (dataString != null && dataString.startsWith(ZXING_URL)) {
                // Scan formats requested in query string (all formats if none specified).
                // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
                source = Source.ZXING_LINK;
                sourceUrl = dataString;
                Uri inputUri = Uri.parse(sourceUrl);
                returnUrlTemplate = inputUri.getQueryParameter(RETURN_URL_PARAM);
                decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);
            } else {
                // Scan all formats and handle the results ourselves (launched from Home).
                source = Source.NONE;
                decodeFormats = null;
            }
            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
        } else {
            source = Source.NONE;
            decodeFormats = null;
            characterSet = null;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        copyToClipboard = prefs.getBoolean(PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true)
                && (intent == null || intent.getBooleanExtra(Intents.Scan.SAVE_HISTORY, true));

        beepManager.updatePrefs();

        inactivityTimer.onResume();

        registerReceiver(receiver,
                new IntentFilter(RI_WebService.BROADCAST_ACTION));

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPausing = true;
        this.glSurfaceView.onPause();
        // TODO zxing injection
        if(isAndar){

            if(cameraHandler != null)
                cameraHandler.stopThreads();
            // Roger's change
            // as we set handler in both modes, we need to release it
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
        }else{
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }

            ZxingCameraManager.get().closeDriver();
        }
        inactivityTimer.onPause();
        // Roger's unregister method
        unregisterReceiver(receiver);
        finish();      // andar do not allow resuming, so it's finish itself on pause
    }


    /**
     * Set a renderer that draws non AR stuff. Optional, may be set to null or omited.
     * and setups lighting stuff.
     * @param customRenderer
     */
    public void setNonARRenderer(OpenGLRenderer customRenderer) {
		renderer.setNonARRenderer(customRenderer);
	}

    /**
     * Avoid that the screen get's turned off by the system.
     */
	public void disableScreenTurnOff() {
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
    			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
	/**
	 * Set's the orientation to landscape, as this is needed by AndAR.
	 */
    public void setOrientation()  {
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
    
    /**
     * Maximize the application.
     */
    public void setFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }
   
    public void setNoTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    } 
    

    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
        // TODO zxing injection
        // zxing timer shutdown
        inactivityTimer.shutdown();
    	System.runFinalization();
    	if(Config.DEBUG)
    		Debug.stopMethodTracing();
    }
    
    

    
    /* (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
    	super.onStop();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
		public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Integer result = extras.getInt("Result", RI_WebService.UNKNOWN_ERR);
        }
    };

    /**
     * Open the camera.
     */
    private void openCamera()  {
    	if (camera == null) {
            // TODO Roger's change. we will try init zxing camera
    		camera = CameraHolder.instance().open();

            if (camera == null) {
                try {
                    throw new IOException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


    		try {
				camera.setPreviewDisplay(mSurfaceHolder);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

	        if(!Config.USE_ONE_SHOT_PREVIEW) {
	        	camera.setPreviewCallback(cameraHandler);

	        } 
			try {
				cameraHandler.init(camera);

                // TODO init resolutions and preview frames for future use in camera manager
                ZxingCameraManager.get().initConfigManager(camera,zxingCamMode);
                cameraHandler.switchTozxing(zxingCamMode);
                toggleGLSurface(zxingCamMode);
                //TODO zxing injection. Here we set handler for zxing camera usage, when we need to scan barcode
                ZxingCameraManager.get().setAndarPreviewCallback(cameraHandler);


                // TODO zxing injection. Initiate handler for barcode scanner
                if (handler == null) {
                    handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
                }


            } catch (RuntimeException e) {
                // Barcode Scanner has seen crashes in the wild of this variety:
                // java.?lang.?RuntimeException: Fail to connect to camera service
                Log.w(TAG, "Unexpected error initializating camera", e);
                displayFrameworkBugMessageAndExit();
                return;
            } catch (Exception e) {
				e.printStackTrace();
            }
    	}
    }

    private void reInitCamera(boolean camMode){
        try {
            cameraHandler.init(camera);
            ZxingCameraManager.get().initConfigManager(camera,camMode);
            cameraHandler.switchTozxing(camMode);
            toggleGLSurface(camMode);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG_ERROR,"error occurred while triggering camera");
        }
    }

    /**
     * Toggle the visibility of GLSurface.
     * It should be shown for andar camera mode, and hidden for zxing camera.
     * @param zxingCamMode toggle flag
     */
    public void toggleGLSurface(boolean zxingCamMode){
        if(zxingCamMode){
            glSurfaceView.setVisibility(View.GONE);
            viewfinderView.setVisibility(View.VISIBLE);
        }else{
            glSurfaceView.setVisibility(View.VISIBLE);
            viewfinderView.setVisibility(View.GONE);
        }


    }

    /**
     * AndAR method for close and release Camera object
     */
    private void closeCamera() {
        if (camera != null) {
        	CameraHolder.instance().keep();
        	CameraHolder.instance().release();
        	camera = null;
        	camStatus.previewing = false;
        }
    }
    
    /**
     * Open the camera and start detecting markers.
     * note: You must assure that the preview surface already exists!
     */
    public void startPreview() {
//    	if(!surfaceCreated) return;// we do not need wait when the GL surface created
    	if(mPausing || isFinishing()) return;
    	if (camStatus.previewing) stopPreview();
    	openCamera();
		camera.startPreview();
		camStatus.previewing = true;
    }




    /**
     * Close the camera and stop detecting markers.
     */
    private void stopPreview() {
        if (camera != null && camStatus.previewing ) {
            camStatus.previewing = false;
            camera.stopPreview();
        }
    }


	/* The GLSurfaceView changed
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
        if(isAndar){
	    	mSurfaceHolder = holder;
	    	if(startPreviewRightAway)
	    		startPreview();
        }

	}

	/* The GLSurfaceView was created
	 * The camera will be opened and the preview started 
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	public void surfaceCreated(SurfaceHolder holder) {

//        surfaceCreated = true;
         // TODO zxing injection
        if(!isAndar){
            if (!hasSurface) {
                hasSurface = true;
                initCamera(holder);
            }
        }
	}

	/* GLSurfaceView was destroyed
	 * The camera will be closed and the preview stopped.
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

        // TODO andar camera close
        if(isAndar){
	        // Surface will be destroyed when we return, so stop the preview.
	        // Because the CameraDevice object is not a shared resource, it's very
	        // important to release it when the activity is paused.
	        stopPreview();
	        closeCamera();
	        mSurfaceHolder = null;

        }
	}
	
	/**
	 * @return  a the instance of the ARToolkit.
	 */
	public ARToolkit getArtoolkit() {
		return artoolkit;
	}	
	
	/**
	 * Take a screenshot. Must not be called from the GUI thread, e.g. from methods like
	 * onCreateOptionsMenu and onOptionsItemSelected. You have to use a asynctask for this purpose.
	 * @return the screenshot
	 */
	public Bitmap takeScreenshot() {
		return renderer.takeScreenshot();
	}	
	
	/**
	 * 
	 * @return the OpenGL surface.
	 */
	public SurfaceView getSurfaceView() {
		return glSurfaceView;
	}


    // TODO zxing injection

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode) {
        inactivityTimer.onActivity();
        lastResult = rawResult;
        historyManager.addHistoryItem(rawResult);
        if (barcode == null) {
            // This is from history -- no saved barcode
            handleDecodeInternally(rawResult, null);
        } else {
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, rawResult);
            BarcodeFormat barcodeFormat = rawResult.getBarcodeFormat();
            Log.i(TAG_INFO, "barcode format is = " + barcodeFormat.toString());

            // TODO we are inject here
            // start recognizer here and make server upload
            // start service
            // send message to service with barcode format
            // show hourglass while downloading image
            // place resource image over the barcode scanned

            // send message task to riWebService
//      String CoID = riWebService.getCustomerID(this, rawResult, barcode);

            // TODO be sure the service is binded
            new DownloadContentTask(rawResult, barcode).execute();


        }
    }

    public String getCustomerID(Result rawResult, Bitmap barcode) {
        // should return JSON array from server response
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        CharSequence displayContents = resultHandler.getDisplayContents();
        BarcodeFormat format = rawResult.getBarcodeFormat();
        Log.d("QR_Decoder", "URL encoded = " + displayContents);
        String mURL = displayContents.toString();


        return mURL.substring(mURL.indexOf("customerId=") + "customerId=".length());
    }


    class DownloadContentTask extends AsyncTask<Void, Void, Integer> {
        private final Result rawResult;
        private final Bitmap barcode;

        DownloadContentTask(Result rawResult, Bitmap barcode) {
            this.rawResult = rawResult;
            this.barcode = barcode;
        }


        @Override
        protected Integer doInBackground(Void... unused) {
            Integer result = RI_WebService.UNKNOWN_ERR;
            String CustomerID = getCustomerID(rawResult, barcode);
            // TODO optimize with String method of string insert
            String url_part1 = "http://ar.mobile-form.com/service/event?customerId=";

            String url = url_part1 + CustomerID;

            try {
                result = GetServerResponse(url);


            } catch (Throwable t) {
                android.util.Log.e("DownloadContentTask", "Exception in GetServerResponse()", t);
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(Void... unused) {
            // not needed here
        }

        @Override
        protected void onPostExecute(Integer result) {
            // fill data from SearchResponse object

            if (result != RI_WebService.RESPONSE_OK || searchResponse == null) {
                // TODO put error message into strings resource
                Toast.makeText(getApplication(), "error occured", Toast.LENGTH_SHORT);
                return;
            }


            // TODO load info to array, to get posibilities for show more info
            String componentType = searchResponse.output.get(0).componentType;
            String downloadUrl = searchResponse.output.get(0).downloadUrl;
            Toast.makeText(getApplicationContext(), "componentType = " + componentType
                    + "downloadUrl = " + downloadUrl
                    ,
                    Toast.LENGTH_LONG).show();


            //      componentType
            if (componentType.equalsIgnoreCase("IMAGE")) {// IMAGE
                barcodeImageView.setVisibility(View.VISIBLE);
                // download image

                Log.d(TAG_DEBUG, "barcodeImageView will load this source = " + downloadUrl);
                imageDownloader.download(downloadUrl, barcodeImageView);
            } else if (componentType.equalsIgnoreCase("MODEL")) { // MODEL
                // TODO reinit camera for andar mode
                zxingCamMode = false;
                reInitCamera(zxingCamMode);
//                if(model == null) {
//                    new ModelLoader().execute();
//                }

            } else if (componentType.equalsIgnoreCase("HTML")) { // HTML
                barcodeBrowser.setVisibility(View.VISIBLE);

                barcodeBrowser.setBackgroundColor(Color.argb(0, 0, 0, 0));

                barcodeBrowser.setBackgroundResource(R.color.transparent);
                barcodeBrowser.loadUrl(downloadUrl);

            }

            statusView.setVisibility(View.GONE);
            viewfinderView.setVisibility(View.GONE);
            resultView.setVisibility(View.VISIBLE);

        }
    }




    public SearchResponse parseServerResponse(InputStream jRespString) {
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(jRespString);
        SearchResponse response = gson.fromJson(reader, SearchResponse.class);
        return response;
        //        return gson.fromJson(reader, SearchResponse.class);
    }

    public Integer GetServerResponse(String url) {

        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode +
                        " while retrieving bitmap from " + url);
                return RI_WebService.UNKNOWN_ERR;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    searchResponse = parseServerResponse(inputStream);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(TAG_DEBUG, "I/O error while retrieving data from " + url, e);
            return RI_WebService.UNKNOWN_ERR;
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(TAG_DEBUG, "Incorrect URL: " + url);
            return RI_WebService.UNKNOWN_ERR;
        } catch (Exception e) {
            getRequest.abort();
            Log.w(TAG_DEBUG, "Error while retrieving data from " + url, e);
            return RI_WebService.UNKNOWN_ERR;
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return RI_WebService.RESPONSE_OK;
    }



    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode   A bitmap of the captured image.
     * @param rawResult The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_image_border));
            paint.setStrokeWidth(3.0f);
            paint.setStyle(Paint.Style.STROKE);
            Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
            canvas.drawRect(border, paint);

            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1]);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat().equals(BarcodeFormat.UPC_A)) ||
                    (rawResult.getBarcodeFormat().equals(BarcodeFormat.EAN_13))) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1]);
                drawLine(canvas, paint, points[2], points[3]);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    canvas.drawPoint(point.getX(), point.getY(), paint);
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
        canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Result rawResult, Bitmap barcode) {
        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                    R.drawable.launcher_icon));
        } else {
            barcodeImageView.setImageBitmap(barcode);
        }

    }


    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            ZxingCameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
            return;
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializating camera", e);
            displayFrameworkBugMessageAndExit();
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);

        // TODO check for  insurance
//        glSurfaceView.setVisibility(View.GONE);

        lastResult = null;
    }

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }


    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }





    // TODO andar model injection
	private class ModelLoader extends AsyncTask<Void, Void, Void> {


    	@Override
    	protected Void doInBackground(Void... params) {

//			Intent intent = getIntent();
//			Bundle data = intent.getExtras();
			int type = TYPE_INTERNAL;
			String modelFileName = "android.obj";
			BaseFileUtil fileUtil= null;
			File modelFile=null;
			switch(type) {
			case TYPE_EXTERNAL:
				fileUtil = new SDCardFileUtil();
				modelFile =  new File(URI.create(modelFileName));
				modelFileName = modelFile.getName();
				fileUtil.setBaseFolder(modelFile.getParentFile().getAbsolutePath());
				break;
			case TYPE_INTERNAL:
				fileUtil = new AssetsFileUtil(getResources().getAssets());
				fileUtil.setBaseFolder("models/");
				break;
			}

			//read the model file:
			if(modelFileName.endsWith(".obj")) {
				ObjParser parser = new ObjParser(fileUtil);
				try {
					if(edu.dhbw.andobjviewer.Config.DEBUG)
						Debug.startMethodTracing("AndObjViewer");
					if(type == TYPE_EXTERNAL) {
						//an external file might be trimmed
						BufferedReader modelFileReader = new BufferedReader(new FileReader(modelFile));
						String shebang = modelFileReader.readLine();
						if(!shebang.equals("#trimmed")) {
							//trim the file:
							File trimmedFile = new File(modelFile.getAbsolutePath()+".tmp");
							BufferedWriter trimmedFileWriter = new BufferedWriter(new FileWriter(trimmedFile));
							Util.trim(modelFileReader, trimmedFileWriter);
							if(modelFile.delete()) {
								trimmedFile.renameTo(modelFile);
							}
						}
					}
					if(fileUtil != null) {
						BufferedReader fileReader = fileUtil.getReaderFromName(modelFileName);
						if(fileReader != null) {
							model = parser.parse("Model", fileReader);
							model3d = new Model3D(model);
						}
					}
					if(edu.dhbw.andobjviewer.Config.DEBUG)
						Debug.stopMethodTracing();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
    		return null;
    	}
    	@Override
    	protected void onPostExecute(Void result) {
    		super.onPostExecute(result);
//    		waitDialog.dismiss();

    		//register model
    		try {
    			if(model3d!=null)
    				artoolkit.registerARObject(model3d);
			} catch (AndARException e) {
				e.printStackTrace();
			}
			startPreview();
    	}
    }

}