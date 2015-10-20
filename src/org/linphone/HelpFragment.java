package org.linphone;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.widget.EditText;
import android.widget.Toast;

import net.hockeyapp.android.Constants;
import net.hockeyapp.android.FeedbackManager;
import net.hockeyapp.android.views.FeedbackView;

import org.linphone.ui.PreferencesListFragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
    }

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
                + " " + Constants.PHONE_MODEL + "\nAndroid:" + Constants.ANDROID_VERSION + " " + "\n\nACE v" + version;
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
            FeedbackManager.showFeedbackActivity(LinphoneActivity.ctx, Uri.fromFile(feedback));
        }
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
     }

    @Override
    public void onDetach() {
        super.onDetach();
    }


}
