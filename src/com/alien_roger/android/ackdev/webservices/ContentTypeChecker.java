package com.alien_roger.android.ackdev.webservices;

import android.os.AsyncTask;

/**
 * Created by IntelliJ IDEA.
 * User: roger
 * Date: 20.05.11
 * Time: 4:42
 * To change this template use File | Settings | File Templates.
 */
public class ContentTypeChecker {
    private static final String LOG_TAG = "ContentTypeChecker";
    private static final String IMAGE_TYPE = "image";
    private static final String HTML_TYPE = "html";
    private static final String VIEO_TYPE = "video";

    public static final int IMAGE_CONTENT = 1;
    public static final int HTML_CONTENT = 2;
    public static final int VIDEO_CONTENT = 3;
    public static final int UNKNOWN_CONTENT = -1;


    public int checkType(String url) {

        return UNKNOWN_CONTENT;
    }


    /**
     * The actual AsyncTask that will asynchronously download the resource.
     */
    public class ContentTypeCheckTask extends AsyncTask<String, Void, Integer> {
        private String url;

        /**
         * Actual download method.
         */
        @Override
        protected Integer doInBackground(String... params) {
            url = params[0];
//            return checkResponse(url);
            return 0;
        }

        /**
         * Once we get response output it
         */
        @Override
        protected void onPostExecute(Integer result) {
            if (isCancelled()) {
                result = -1;
            }
        }
    }
}
