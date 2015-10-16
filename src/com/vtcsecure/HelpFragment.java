package com.vtcsecure;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;

import com.vtcsecure.ui.PreferencesListFragment;

import net.hockeyapp.android.FeedbackManager;

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
        FeedbackManager.showFeedbackActivity(LinphoneActivity.ctx);
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
