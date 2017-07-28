package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Bob on 17/03/2017.
 * Android does not allow Network IO to happen in the Main Thread,
 * we need to run it in its own Thread and a common way to do this is with AsyncTask.
 */

public class CmsApiCall extends AsyncTask<String, Void, String> {

    private Context mContext;
    public AsyncResponse delegate = null;
    private boolean hasError = false;
    private boolean mForSwipeToRefresh  = false;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Account mCurrentAccount = null;

    public interface AsyncResponse {
        void processFinish(String output) throws JSONException;
    }

    public CmsApiCall(Context context, SwipeRefreshLayout swipeRefreshLayout, Account account, AsyncResponse delegate){
        this.delegate = delegate;
        mContext = context;
        mSwipeRefreshLayout = swipeRefreshLayout;
        mCurrentAccount = account;
        mForSwipeToRefresh = true;
    }

    public CmsApiCall(Context context, Account account, AsyncResponse delegate){
        this.delegate = delegate;
        mContext = context;
        mForSwipeToRefresh = false;
        mCurrentAccount = account;
    }

    public CmsApiCall(Context context, AsyncResponse delegate){
        this.delegate = delegate;
        mContext = context;
        mForSwipeToRefresh = false;
    }


    @Override
    protected void onPreExecute() {
        if(mForSwipeToRefresh){
            mSwipeRefreshLayout.setRefreshing(true);
        }
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        String result = "";
        try {
            String urlstring = params[0];
            Log.i("SERC Log:", "HTTP Connecting: " + urlstring);

            // Recommended way of making http requests is HttpURLConnection
            URL url = new URL(urlstring);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                InputStream reader = new BufferedInputStream(urlConnection.getInputStream());
                Log.i("SERC Log:", "Starting to read text");
                String text = "";
                int i = 0;
                while ((i = reader.read()) != -1) {
                    text += (char) i;
                }
                Log.i("SERC Log:", "HTTP Response: " + text);
                result = text;

            } catch (Exception e) {
                Log.i("SERC Log:", "HTTP Exception: " + e);
                hasError = true;

            } finally {
                Log.i("SERC Log:", "HTTP Disconnecting");
                urlConnection.disconnect();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("SERC Log:", "HTTP Exception: " + e);
            hasError = true;

        }

        return result;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (hasError){
            // Alert user
            if (mCurrentAccount != null) {
                Toast.makeText(mContext, "Error. Could not fetch data from " + mCurrentAccount.getAccountName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Error. Could not fetch data", Toast.LENGTH_SHORT).show();
            }
            if (mForSwipeToRefresh) {
                stopRefreshLayoutRefreshing();
            }
        }
        try {
            delegate.processFinish(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void stopRefreshLayoutRefreshing(){
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }



}

