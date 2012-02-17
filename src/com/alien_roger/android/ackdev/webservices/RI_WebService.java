package com.alien_roger.android.ackdev.webservices;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.alien_roger.android.ackdev.zxing.BarcodeFormat;
import com.alien_roger.android.ackdev.zxing.Result;
import com.alien_roger.android.ackdev.zxing.client.android.AndroidHttpClient;
import com.alien_roger.android.ackdev.zxing.client.android.result.ResultHandler;
import com.alien_roger.android.ackdev.zxing.client.android.result.ResultHandlerFactory;
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

/**
 * Created by IntelliJ IDEA.
 * User: roger
 * Date: 23.05.11
 * Time: 5:36
 * To change this template use File | Settings | File Templates.
 */
public class RI_WebService extends Service {
    private static final String TAG_DEBUG = "AckDev_RI_Service_DEBUG";

    public static final int RESPONSE_OK = 0;
    public static final int UNKNOWN_ERR = 4;

    public static final int NOTIF_ID = 1337;
    public static final String SHUTDOWN = "SHUTDOWN";
    public static final String BROADCAST_ACTION =
            "com.alien_roger.android.service.ResponseRecognizedEvent";
    private String mURL;
    private Activity activity;

    //	private LocationManager mgr=null;
//	private String forecast=null;
//	private HttpClient client=null;
//	private String format=null;
//	private Intent broadcast=new Intent(BROADCAST_ACTION);
    private final Binder binder = new LocalBinder();
    private SearchResponse searchResponse;


    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return (binder);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//		mgr.removeUpdates(onLocationChange);
    }


//	private void updateForecast(Location loc) {
//		new DownloadContentTask().execute(loc);
//	}

    public void getCodeResource(Activity activity, Result rawResult, Bitmap barcode, SearchResponse searchResp) {
        this.searchResponse = searchResp;
        new DownloadContentTask(activity, rawResult, barcode).execute();
    }


    public String getCustomerID(Activity activity, Result rawResult, Bitmap barcode) {
        String coID = "";
        // should return JSON array from server response

        this.activity = activity;
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(activity, rawResult);
        CharSequence displayContents = resultHandler.getDisplayContents();
        BarcodeFormat format = rawResult.getBarcodeFormat();
        Log.d("QR_Decoder", "URL encoded = " + displayContents);
        mURL = displayContents.toString();

//          market://packagename=com.ackdev.interativereality&customerId=
//        URL=market://packagename=com.ackdev.interativereality&coid=9010
//        http://2.arengineserver.appspot.com/download?id=coid
        return coID = mURL.substring(mURL.indexOf("customerId=") + "customerId=".length());
    }


    public class LocalBinder extends Binder {
        public RI_WebService getService() {
            return (RI_WebService.this);
        }
    }


    public void parseServerResponse(InputStream jRespString, SearchResponse response) {
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(jRespString);
        response = gson.fromJson(reader, SearchResponse.class);
//          return response;
//        return gson.fromJson(reader, SearchResponse.class);
    }


    class DownloadContentTask extends AsyncTask<Void, Void, Integer> {

        private Result rawResult;
        private Bitmap barcode;
        private Activity activity;
//      private SearchResponse searchResponse;


        DownloadContentTask(Activity activity, Result rawResult, Bitmap barcode) {
            this.activity = activity;
            this.rawResult = rawResult;
            this.barcode = barcode;
//          this.searchResponse = searchResp;
        }


        @Override
        protected Integer doInBackground(Void... unused) {
            Integer result = UNKNOWN_ERR;
            String CustomerID = getCustomerID(activity, rawResult, barcode);
            // TODO optimize with String method of string insert
            String url_part1 = "http://ar.mobile-form.com/service/event?customerId=";

            String url = url_part1 + CustomerID;

            try {
                result = GetServerResponse(url, searchResponse);

                Intent broadcast = new Intent(BROADCAST_ACTION);
                broadcast.putExtra("Result", result);
                sendBroadcast(broadcast);
            } catch (Throwable t) {
                android.util.Log.e("WeatherPlus", "Exception in updateForecast()", t);
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(Void... unused) {
            // not needed here
        }

        @Override
        protected void onPostExecute(Integer result) {

        }
    }

    public Integer GetServerResponse(String url, SearchResponse resp) {

        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode +
                        " while retrieving bitmap from " + url);
                return UNKNOWN_ERR;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
//                      resp = parseServerResponse(inputStream,resp);
                    parseServerResponse(inputStream, resp);
//                    if(resp.error != null){
//                        return resp.error;
//                    }else{
//                        saveJSON_Obj(resp);
//                        return RESPONSE_OK;
//                    }

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
            return UNKNOWN_ERR;
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(TAG_DEBUG, "Incorrect URL: " + url);
            return UNKNOWN_ERR;
        } catch (Exception e) {
            getRequest.abort();
            Log.w(TAG_DEBUG, "Error while retrieving data from " + url, e);
            return UNKNOWN_ERR;
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return RESPONSE_OK;
    }

}
