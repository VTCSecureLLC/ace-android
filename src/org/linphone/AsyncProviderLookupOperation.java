package org.linphone;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import org.linphone.setup.CDNProviders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;




/**
 * Created by zackmatthews on 1/20/16.
 */
//Helper class to pull all provider domains from the CDN, pass into setup.login for autoconfig
public class AsyncProviderLookupOperation extends AsyncTask<Void, Void, Void> {

    final String cdnProviderList = "https://cdn.vatrp.net/domains.json";
    //  protected SharedPreferences sharedPreferences;
    protected Context context;
    protected ArrayList<ProviderNetworkOperationListener> listeners;
    private static AsyncProviderLookupOperation runingInstanceProviderLookupOperation;
    public static boolean isAsyncTaskRuning = false;

    public AsyncProviderLookupOperation(ProviderNetworkOperationListener mListener, Context context) {
        this.context = context;
        listeners = new ArrayList<ProviderNetworkOperationListener>();
        if(mListener!=null)
            listeners.add(mListener);
    }

    public void addListener(ProviderNetworkOperationListener mListener) {
        listeners.add(mListener);
    }
    public void removeListener(ProviderNetworkOperationListener mListener)
    {
        listeners.remove(mListener);
    }

    public interface ProviderNetworkOperationListener {
        void onProviderLookupFinished();
    };

    public static AsyncProviderLookupOperation getInstance(){
        return runingInstanceProviderLookupOperation;
    }

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
        org.linphone.mediastream.Log.d("ttt reloadProviderDomains");
        String textjson = "";
        try {
            textjson = getText(cdnProviderList);

            CDNProviders providers = CDNProviders.getInstance();
            CDNProviders.getInstance().updateProviders(textjson, true);

            for (int i=0; i<providers.getProvidersCount(); i++)
            {
                CDNProviders.Provider p = providers.getProvider(i);
                downloadImagesToSdCard(p.getIcon2x(), i);
                org.linphone.mediastream.Log.e("ttt p.getIcon2x()" + p.getIcon2x());
            }

            org.linphone.mediastream.Log.d("textjson=" + textjson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(isAsyncTaskRuning) {
            cancel(true);
            return;
        }
        isAsyncTaskRuning = true;
        runingInstanceProviderLookupOperation = this;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if(isCancelled()) return null;
        else
        reloadProviderDomains();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        runingInstanceProviderLookupOperation = null;
        isAsyncTaskRuning = false;
        for (ProviderNetworkOperationListener listener: listeners
             ) {
            if(listener!= null)
                listener.onProviderLookupFinished();
        }
        listeners.clear();

    }



    private void downloadImagesToSdCard(String downloadUrl, int index)
    {
        try
        {
            URL url = new URL(downloadUrl);
                        /* making a directory in sdcard */
            String sdCard = Environment.getExternalStorageDirectory().toString()+ "/ACE/icons";

                        /* checks the file and if it already exist delete */
            String fname = index+ ".png";
            File file = new File (sdCard, fname);
            if (!file.exists ())
                file.getParentFile().mkdirs();
            else
                file.delete();

                             /* Open a connection */
            URLConnection ucon = url.openConnection();
            InputStream inputStream = null;
            HttpURLConnection httpConn = (HttpURLConnection)ucon;
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                inputStream = httpConn.getInputStream();
            }

            FileOutputStream fos = new FileOutputStream(file);
            int totalSize = httpConn.getContentLength();
            int downloadedSize = 0;
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ( (bufferLength = inputStream.read(buffer)) >0 )
            {
                fos.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;

            }

            fos.close();
        }
        catch(IOException io)
        {
            io.printStackTrace();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}