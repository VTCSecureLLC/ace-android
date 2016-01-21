package org.linphone;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by zackmatthews on 1/20/16.
 */
//Helper class to pull all provider domains from the CDN, pass into setup.login for autoconfig
public class AsyncProviderLookupOperation extends AsyncTask<Void, Void, Void> {

    final String cdnProviderList = "http://cdn.vatrp.net/new-domains.json";
    protected SharedPreferences sharedPreferences;
    protected Context context;
    protected ProviderNetworkOperationListener listener;
    protected ArrayList<String> domains;

    public AsyncProviderLookupOperation(ProviderNetworkOperationListener mListener, Context context) {
        this.context = context;
        this.listener = mListener;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public interface ProviderNetworkOperationListener {
        void onProviderLookupFinished(ArrayList<String> mDomains);
    };

    protected String getText(String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        return response.toString();
    }

    protected void reloadProviderDomains() {
        String textjson = "";
        domains = new ArrayList<String>();
        try {
            textjson = getText(cdnProviderList);
            org.linphone.mediastream.Log.d("textjson=" + textjson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONArray reader = null;
        try {
            reader = new JSONArray(textjson);
            for (int i = 0; i < reader.length(); i++) {
                //Store CDN providers and their domains in SharedPreferences
                sharedPreferences.edit().
                        putString("provider" + String.valueOf(i), ((JSONObject) reader.get(i)).getString("name")).commit();
                sharedPreferences.edit().
                        putString("provider" + String.valueOf(i) + "domain", ((JSONObject) reader.get(i)).getString("domain")).commit();
                domains.add(((JSONObject) reader.get(i)).getString("name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        reloadProviderDomains();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        listener.onProviderLookupFinished(domains);
    }
}