package org.linphone;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import net.hockeyapp.android.Constants;
import net.hockeyapp.android.FeedbackManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.mediastream.Log;
import org.linphone.ui.PreferencesListFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * A fragment representing a list of Items.
 * <p/>
  */
public class HelpFragment extends PreferencesListFragment {


   public static HelpFragment newInstance() {

        HelpFragment fragment = new HelpFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HelpFragment() {
        super(R.xml.help);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference("instantfeedbackace").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (LinphoneActivity.isInstanciated()) {
                    showFeedbackActivity();
                    return true;
                }
                return false;
            }
        });
        new LoadWebPageASYNC().execute();
    }
    public void populate_deaf_and_hard_of_hearing_preference() throws JSONException {
        String textjson;
        try {
           textjson=getText("http://cdn.vatrp.net/numbers.json");
            Log.d("textjson="+textjson);
        } catch (Exception e) {
            e.printStackTrace();
            textjson="[\n" +
                    "  {\"name\":\"SBA ASL Line\", \"address\":\"+18554404960\"},\n" +
                    "  {\"name\":\"FCC ASL Line\", \"address\":\"+18444322275\"}\n" +
                    "]";
        }

        JSONArray reader = new JSONArray(textjson);


        PreferenceScreen hoh_screen=(PreferenceScreen)findPreference("websiteace");
        for(int i=0; i<reader.length(); i++){
            Preference pref=new Preference(getActivity());
            pref.setKey("hoh_item" + String.valueOf(i));
            pref.setTitle(((JSONObject) reader.get(i)).getString("name"));
            pref.setSummary(((JSONObject) reader.get(i)).getString("address"));
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (LinphoneActivity.isInstanciated()) {

                        LinphoneManager.getInstance().newOutgoingCall(preference.getSummary().toString(),preference.getTitle().toString());

                        return true;
                    }
                    return false;
                }
            });
            hoh_screen.addPreference(pref);
        }


    };
    public void showFeedbackActivity() {
        FeedbackManager.register(LinphoneActivity.ctx, "d6280d4d277d6876c709f4143964f0dc", null);

        String version;
        try {
            PackageManager manager = getActivity().getPackageManager();
            PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
            version  = String.valueOf(info.versionName);
        }

        catch(PackageManager.NameNotFoundException e){
            version = "Beta";
        }

        String devStats = "\n\n" + Constants.PHONE_MANUFACTURER
                + " " + Constants.PHONE_MODEL + "\nAndroid:" + Constants.ANDROID_VERSION + " " + "\nACE v" + version + "\n\n" + getFeedbackLogs();
        File feedback = null;
        try
        {
            File root = new File(Environment.getExternalStorageDirectory(), "ACE");
            if (!root.exists()) {
                root.mkdirs();
            }
            feedback = new File(root, "hockeyAppFeedback.txt");
            FileWriter writer = new FileWriter(feedback);
            writer.append(devStats);
            writer.flush();
            writer.close();
            Toast.makeText(LinphoneActivity.ctx, "Saved", Toast.LENGTH_SHORT).show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        if(feedback != null) {
            ///storage/emulated/0/ACE/hockeyAppFeedback.txt
	        File crashFeedbackFile = new File(Environment.getExternalStorageDirectory() +"/ACE/hockeyAppCrashFeedback.txt");
	        if(crashFeedbackFile.exists() && crashFeedbackFile.isFile())
		        FeedbackManager.showFeedbackActivity(LinphoneActivity.ctx, Uri.fromFile(feedback), Uri.fromFile(crashFeedbackFile));
	        else
		        FeedbackManager.showFeedbackActivity(LinphoneActivity.ctx, Uri.fromFile(feedback));
        }
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
     }
    public static String getText(String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        return response.toString();
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }

    public String getFeedbackLogs(){

        String feedbackLogs = "";

        try {
            Process process = Runtime.getRuntime().exec( "logcat -d Linphone:I LinphoneCoreFactoryImpl:I WEBRTC-JR:W *:S");

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
                log.append(System.getProperty("line.separator"));
            }
            bufferedReader.close();

            feedbackLogs = System.getProperty("line.separator") + log.toString();
        }
        catch (IOException e) {
        }

	   int startOfLastThousandLines = feedbackLogs.length();

	     for (int i = 0; i < 1000; i++) {
		     if(startOfLastThousandLines == 0)
			     break;
		     else
		    startOfLastThousandLines = feedbackLogs.lastIndexOf('\n', startOfLastThousandLines - 1);
	    }

        return feedbackLogs.substring(startOfLastThousandLines);
    }
    private class LoadWebPageASYNC extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                populate_deaf_and_hard_of_hearing_preference();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

        }

    }

}
