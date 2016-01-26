package org.linphone;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;

import org.linphone.setup.CDNProviders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by zackmatthews on 1/20/16.
 */
//Helper class to pull all provider domains from the CDN, pass into setup.login for autoconfig
public class AsyncProviderLookupOperation extends AsyncTask<Void, Void, Void> {

    final String cdnProviderList = "http://cdn.vatrp.net/domains.json";
    //  protected SharedPreferences sharedPreferences;
    protected Context context;
    protected ProviderNetworkOperationListener listener;

    public AsyncProviderLookupOperation(ProviderNetworkOperationListener mListener, Context context) {
        this.context = context;
        this.listener = mListener;
    }

    public interface ProviderNetworkOperationListener {
        void onProviderLookupFinished();
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
        try {
            textjson = getText(cdnProviderList);
            CDNProviders providers = CDNProviders.getInstance();
            CDNProviders.getInstance().updateProviders(textjson, true);

            for (int i=0; i<providers.getProvidersCount(); i++)
            {
                CDNProviders.Provider p = providers.getProvider(i);
                imageLoader(p.getIcon2x(), i);
            }

            org.linphone.mediastream.Log.d("textjson=" + textjson);
        } catch (Exception e) {
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
        listener.onProviderLookupFinished();
    }

    public void copyStream(InputStream is, int index) throws FileNotFoundException{

        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File file = new File(directory, String.valueOf(index) + ".png");
        if(!file.exists())
            file.mkdirs();
        FileOutputStream ostream = null;


        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            for (; ; ) {
                int count = is.read(bytes, 0, buffer_size);
                if (count == -1)
                    break;
                ostream.write(bytes, 0, count);
            }
        } catch (Exception ex) {
        }
    }

    void imageLoader(String url, int index) {
        // Download Images from the Internet
        try {


            URL imageUrl = new URL(url.replace(" ", "%20"));
            HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            InputStream is = conn.getInputStream();
            copyStream(is, index);
            conn.disconnect();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}