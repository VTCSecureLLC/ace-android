package org.linphone;

import android.app.Activity;
import android.os.Bundle;


import org.linphone.ui.PreferencesListFragment;

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
