/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alien_roger.android.ackdev.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
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
import com.alien_roger.android.ackdev.zxing.client.android.camera.CameraManager;
import com.alien_roger.android.ackdev.zxing.client.android.history.HistoryManager;
import com.alien_roger.android.ackdev.zxing.client.android.result.ResultHandler;
import com.alien_roger.android.ackdev.zxing.client.android.result.ResultHandlerFactory;
import com.alien_roger.android.ackdev.zxing.client.android.share.ShareActivity;
import com.google.gson.Gson;
import com.javacodegeeks.android.json.model.SearchResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

//import com.alien_roger.android.ackdev.zxing.client.android.camera.CameraStatus;

//import edu.dhbw.andar.*;

/**
 * The barcode reader activity itself. This is loosely based on the CameraPreview
 * example included in the Android SDK.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
//public final class CaptureActivity  extends AndARActivity implements SurfaceHolder.Callback {
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {


    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final String TAG_INFO = CaptureActivity.class.getSimpleName() + "_INFO";
    private static final String TAG_DEBUG = CaptureActivity.class.getSimpleName() + "_DEBUG";

    private static final int SHARE_ID = Menu.FIRST;
    private static final int HISTORY_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int HELP_ID = Menu.FIRST + 3;
    private static final int ABOUT_ID = Menu.FIRST + 4;

    private static final long INTENT_RESULT_DURATION = 1500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private static final String PACKAGE_NAME = "com.alien_roger.android.ackdev.zxing.client.android";
    private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";
    private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";
    private static final String ZXING_URL = "http://zxing.appspot.com/scan";
    private static final String RETURN_CODE_PLACEHOLDER = "{CODE}";
    private static final String RETURN_URL_PARAM = "ret";

    private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;

    // Roger's
    private final ImageDownloader imageDownloader = new ImageDownloader();



//    private GLSurfaceView mGLSurfaceView;
//    private boolean testFlag = true;



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
    private RI_WebService riWebService = null;
    private SearchResponse searchResponse;









    // andar and zxing devider
    // set flag that will toggle camera's
    private boolean isAndar = false;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(!isAndar){
            // zxing init
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.capture);
            // zxing cameraManager and surfaceViews init
            CameraManager.init(getApplication());
            // TODO Roger's change
//            viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
            resultView = findViewById(R.id.result_view);
            statusView = (TextView) findViewById(R.id.status_view);
            // handlers and Managers for zxing
            handler = null;
            lastResult = null;
            hasSurface = false;
            historyManager = new HistoryManager(this);
            historyManager.trimHistory();
            inactivityTimer = new InactivityTimer(this);
            beepManager = new BeepManager(this);
        }


        // Roger's additional overlays view for image and html
        barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        barcodeBrowser = (WebView) findViewById(R.id.webkit);
        // JSON parser response class
        searchResponse = new SearchResponse();


        // test 3D model with GLSurface
//        // GL Surface Implementation
//        // Create our Preview view and set it as the content of our
//        // Activity
////        mGLSurfaceView = new GLSurfaceView(this);
//        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
//        // We want an 8888 pixel format because that's required for
//        // a translucent window.
//        // And we want a depth buffer.
//        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
//        // Tell the cube renderer that we want to render a translucent version
//        // of the cube:
//        mGLSurfaceView.setRenderer(new CubeRenderer(true));
//        // Use a surface format with an Alpha channel:
//        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);





        // zxing first launch help method
//    showHelpOnFirstLaunch();

    }


    @Override
    protected void onResume() {
        super.onResume();
        if(isAndar){

        }else{
            // zxing part surfaceView callbacks init
            resetStatusView();

            // TODO ROger's change
//            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceView surfaceView = new SurfaceView(CaptureActivity.this);
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
        }
//      }


        // register receiver for service
        registerReceiver(receiver,
                new IntentFilter(RI_WebService.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(isAndar){

        }else{
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            inactivityTimer.onPause();
            CameraManager.get().closeDriver();
        }

//      }




        // Roger's unregister method
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStart() {
        super.onStart();


        // Roger's service bind
        bindService(new Intent(this, RI_WebService.class),
                onServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if(isAndar){



        }else{
            // zxing timer shutdown
            inactivityTimer.shutdown();
        }


        // Roger's service unbind
        unbindService(onServiceConnection);
        super.onDestroy();

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (source == Source.NATIVE_APP_INTENT) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            } else if ((source == Source.NONE || source == Source.ZXING_LINK) && lastResult != null) {
                resetStatusView();
                if (handler != null) {
                    handler.sendEmptyMessage(R.id.restart_preview);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // Handle these events so they don't launch the Camera app
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SHARE_ID, 0, R.string.menu_share)
                .setIcon(android.R.drawable.ic_menu_share);
        menu.add(0, HISTORY_ID, 0, R.string.menu_history)
                .setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(0, SETTINGS_ID, 0, R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, HELP_ID, 0, R.string.menu_help)
                .setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, ABOUT_ID, 0, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    // Don't display the share menu item if the result overlay is showing.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(SHARE_ID).setVisible(lastResult == null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SHARE_ID: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setClassName(this, ShareActivity.class.getName());
                startActivity(intent);
                break;
            }
            case HISTORY_ID: {
                AlertDialog historyAlert = historyManager.buildAlert();
                historyAlert.show();
                break;
            }
            case SETTINGS_ID: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setClassName(this, PreferencesActivity.class.getName());
                startActivity(intent);
                break;
            }
            case HELP_ID: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setClassName(this, HelpActivity.class.getName());
                startActivity(intent);
                break;
            }
            case ABOUT_ID:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.title_about) + versionName);
                builder.setMessage(getString(R.string.msg_about) + "\n\n" + getString(R.string.zxing_url));
                builder.setIcon(R.drawable.launcher_icon);
                builder.setPositiveButton(R.string.button_open_browser, aboutListener);
                builder.setNegativeButton(R.string.button_cancel, null);
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /* The GLSurfaceView was created
      * The camera will be opened and the preview started
      * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
      */
    public void surfaceCreated(SurfaceHolder holder) {
        //
        if(isAndar){

        }else{
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
    }


    /* The GLSurfaceView changed
      * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
      */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

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


    class DownloadContentTask extends AsyncTask<Void, Void, Integer> {
        private Result rawResult;
        private Bitmap barcode;

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
            } else if (componentType.equalsIgnoreCase("MODEL")) { // HTML


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
/*
        TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());

        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String formattedTime = formatter.format(new Date(rawResult.getTimestamp()));
        TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
        timeTextView.setText(formattedTime);


        TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
        View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
        metaTextView.setVisibility(View.GONE);
        metaTextViewLabel.setVisibility(View.GONE);
        Map<ResultMetadataType, Object> metadata =
                rawResult.getResultMetadata();
        if (metadata != null) {
            StringBuilder metadataText = new StringBuilder(20);
            for (Map.Entry<ResultMetadataType, Object> entry : metadata.entrySet()) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
                    metadataText.append(entry.getValue()).append('\n');
                }
            }
            if (metadataText.length() > 0) {
                metadataText.setLength(metadataText.length() - 1);
                metaTextView.setText(metadataText);
                metaTextView.setVisibility(View.VISIBLE);
                metaTextViewLabel.setVisibility(View.VISIBLE);
            }
        }

        // here we got encoded URL

        TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
        CharSequence displayContents = resultHandler.getDisplayContents();
        contentsTextView.setText(displayContents);
        // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
        int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
        contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

        TextView supplementTextView = (TextView) findViewById(R.id.contents_supplement_text_view);
        supplementTextView.setText("");
        supplementTextView.setOnClickListener(null);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_SUPPLEMENTAL, true)) {
            SupplementalInfoRetriever.maybeInvokeRetrieval(supplementTextView, resultHandler.getResult(),
                    handler, this);
        }

        int buttonCount = resultHandler.getButtonCount();
        ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
        buttonView.requestFocus();
        for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
            TextView button = (TextView) buttonView.getChildAt(x);
            if (x < buttonCount) {
                button.setVisibility(View.VISIBLE);
                button.setText(resultHandler.getButtonText(x));
                button.setOnClickListener(new ResultButtonListener(resultHandler, x));
            } else {
                button.setVisibility(View.GONE);
            }
        }

        if (copyToClipboard) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(displayContents);
        }*/
    }

    // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
    private void handleDecodeExternally(Result rawResult, Bitmap barcode) {
        viewfinderView.drawResultBitmap(barcode);

        // Since this message will only be shown for a second, just tell the user what kind of
        // barcode was found (e.g. contact info) rather than the full contents, which they won't
        // have time to read.
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        statusView.setText(getString(resultHandler.getDisplayTitle()));

        if (copyToClipboard) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(resultHandler.getDisplayContents());
        }

        if (source == Source.NATIVE_APP_INTENT) {
            // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
            // the deprecated intent is retired.
            Intent intent = new Intent(getIntent().getAction());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
            byte[] rawBytes = rawResult.getRawBytes();
            if (rawBytes != null && rawBytes.length > 0) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
            }
            Message message = Message.obtain(handler, R.id.return_scan_result);
            message.obj = intent;
            handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
        } else if (source == Source.PRODUCT_SEARCH_LINK) {
            // Reformulate the URL which triggered us into a query, so that the request goes to the same
            // TLD as the scan URL.
            Message message = Message.obtain(handler, R.id.launch_product_query);
            int end = sourceUrl.lastIndexOf("/scan");
            message.obj = sourceUrl.substring(0, end) + "?q=" +
                    resultHandler.getDisplayContents().toString() + "&source=zxing";
            handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
        } else if (source == Source.ZXING_LINK) {
            // Replace each occurrence of RETURN_CODE_PLACEHOLDER in the returnUrlTemplate
            // with the scanned code. This allows both queries and REST-style URLs to work.
            Message message = Message.obtain(handler, R.id.launch_product_query);
            message.obj = returnUrlTemplate.replace(RETURN_CODE_PLACEHOLDER,
                    resultHandler.getDisplayContents().toString());
            handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
        }
    }

    /**
     * We want the help screen to be shown automatically the first time a new version of the app is
     * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
     * it to a value stored as a preference.
     */
    private boolean showHelpOnFirstLaunch() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
            int currentVersion = info.versionCode;
            // Since we're paying to talk to the PackageManager anyway, it makes sense to cache the app
            // version name here for display in the about box later.
            this.versionName = info.versionName;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
            if (currentVersion > lastVersion) {
                prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
                Intent intent = new Intent(this, HelpActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                // Show the default page on a clean install, and the what's new page on an upgrade.
                String page = (lastVersion == 0) ? HelpActivity.DEFAULT_PAGE : HelpActivity.WHATS_NEW_PAGE;
                intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
                startActivity(intent);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
        }
        return false;
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // TODO we will update display content here by reading intent

            Bundle extras = intent.getExtras();
            Integer result = extras.getInt("Result", RI_WebService.UNKNOWN_ERR);


            showContent(result);
        }
    };

    private void showContent(Integer result) {
        if (result != RI_WebService.RESPONSE_OK || searchResponse == null) {
            // TODO put error message into strings resource
            Toast.makeText(getApplication(), "error occured", Toast.LENGTH_SHORT);
            return;
        }

        // TODO load info to array, to get posibilities for show more info
        String componentType = searchResponse.output.get(0).componentType;
        String downloadUrl = searchResponse.output.get(0).downloadUrl;

//      componentType
        if (componentType.equalsIgnoreCase("IMAGE")) {// IMAGE
            barcodeImageView.setVisibility(View.VISIBLE);
            // download image

            Log.d(TAG_DEBUG, "barcodeImageView will load this source = " + downloadUrl);
            imageDownloader.download(downloadUrl, barcodeImageView);
        } else { // HTML
            barcodeBrowser.setVisibility(View.VISIBLE);
            String downloadLink = "http://stackoverflow.com/questions/1260422/setting-webview-background-image-to-a-resource-graphic-in-android";


            barcodeBrowser.setBackgroundColor(Color.argb(0, 0, 0, 0));

            barcodeBrowser.setBackgroundResource(R.drawable.icon);
            barcodeBrowser.loadUrl(downloadLink);

        }


        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);
    }

    private ServiceConnection onServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            riWebService = ((RI_WebService.LocalBinder) rawBinder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            riWebService = null;
        }
    };


    private final DialogInterface.OnClickListener aboutListener =
            new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.zxing_url)));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                }
            };

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }


    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
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
//    mGLSurfaceView.setVisibility(View.VISIBLE);

        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }
}
