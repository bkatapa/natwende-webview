package com.mweka.natwendemobile;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static transient final String sBaseUrl = "http://LAPTOP-PK9QE32K:8080/natwende-mobile".toLowerCase();
    private static transient final String sErrorUrl = "file:///android_asset/error.html";
    private static transient final String sSplashUrl = "file:///android_asset/loading.html";
    private static final String TAG = "MainActivity";

    private WebView mWebView;
    private boolean clearHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setContentView(R.layout.activity_main);
        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        //mWebView.getSettings().setAppCacheEnabled(false);
        //mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.setWebViewClient(mClient);
        boolean hasConnectivity = hasNetworkConnection();
        //mWebView.loadUrl(result ? sBaseUrl : sErrorUrl);
        //mWebView.loadUrl(hasNetworkConnection() ? sBaseUrl : sErrorUrl);

        try {
            URL url = new URL(sBaseUrl);
            //boolean isHostReachable = isOnline("/system/bin/ping -c 1 " + url.getHost());
            //boolean isServerReachable = isOnline("/system/bin/curl -i " + sBaseUrl);
            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
            if (!hasConnectivity) {
                mWebView.loadUrl(sErrorUrl);
            }
            else {
                mWebView.loadUrl(sSplashUrl);
                clearHistory = true;
                new CheckHostAvailabilityTask().execute();
            }
            //mWebView.loadUrl(hasNetworkConnection() && isOnline(url) ? sBaseUrl : sErrorUrl);
        }
        catch (java.net.MalformedURLException ex) {
            ex.printStackTrace();
            mWebView.loadUrl(sErrorUrl);
        }

        setContentView(mWebView);
    }

    private WebViewClient mClient = new WebViewClient() {
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final String url = request.getUrl().toString();
            Log.i(TAG, "shouldOverrideUrlLoading() — url is: " + url);
            if (url.startsWith(sBaseUrl)) {
                return false;
            }
            else {
                Log.i(TAG, "shouldOverrideUrlLoading() — url loading failed: " + url);
            }
            return showToast("Navigation failed [" + url + "]");
        }
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "shouldOverrideUrlLoading() — url is: " + url);
            if (url.startsWith(sBaseUrl)) {
                return false;
            }
            else {
                Log.i(TAG, "shouldOverrideUrlLoading() — failed to load url: " + url);
            }
            return showToast("Navigation failed [" + url + "]");
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.i(TAG, "onPageStarted() — url is: " + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG, "Entering onPageFinished() — url is: " + url);
            if (clearHistory && (url.startsWith(sBaseUrl) || url.startsWith(sErrorUrl))) {
                Log.i(TAG, "onPageFinished(), clearing history... url is: " + url);
                clearHistory = false;
                mWebView.clearHistory();
            }
            super.onPageFinished(view, url);
            Log.i(TAG, "Exiting onPageFinished() — url is: " + url);
        }

        private boolean showToast(String msg) {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            return true;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "Entering onKeyDown() — event is: " + event.getCharacters());
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    private boolean hasNetworkConnection() {
        Log.i(TAG, "Entering hasNetworkConnection()");

        boolean hasConnectedWifi = false;
        boolean hasConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();

        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    hasConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    hasConnectedMobile = true;
        }
        Log.i(TAG, "Exiting hasNetworkConnection(), hasConnectedMobile = " + hasConnectedMobile
                + ", hasConnectedWifi = " + hasConnectedWifi);
        return hasConnectedWifi || hasConnectedMobile;
    }

    // ICMP

    public boolean isOnline(String cmd) {
        Log.i(TAG, "Entering isOnline(), cmd is: " + cmd);
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec(cmd);
            int     exitValue = ipProcess.waitFor();
            Log.i(TAG, "isOnline(), exitValue is: " + exitValue);
            return (exitValue == 0);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOnline(URL uri) {
        Log.i(TAG, "Entering isOnline(), URI is: " + uri);
        try {
            HttpURLConnection con = (HttpURLConnection) uri.openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(5000); //set timeout to 5 seconds
            int responseCode = con.getResponseCode();
            Log.i(TAG, "isOnline(), HTTP response code is: " + responseCode);
            return responseCode == HttpURLConnection.HTTP_OK;
        }
        catch (Exception e) {
            Log.e(TAG, "isOnline(), HTTP connection call error", e);
            e.printStackTrace();
        }
        return false;
    }

    private class CheckHostAvailabilityTask extends AsyncTask<Void, Void, Boolean> {
        private final String TAG = CheckHostAvailabilityTask.class.getName();
        private URL url;
        @Override
        protected Boolean doInBackground(Void...params) {
            try {
                url = new URL(sBaseUrl);
                Socket sock = new Socket();
                sock.connect(new InetSocketAddress(url.getHost(), 53), 1500);
                sock.close();
                return true;
            }
            catch (IOException e) {
                Log.e(TAG, "doInBackground(), socket connection error: ", e);
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean isReachable) {
            Log.i(TAG, "onPostExecute(), isReachable: " + isReachable);
            mWebView.loadUrl(isReachable || isOnline(url) ? sBaseUrl : sErrorUrl);
        }
    }
}
