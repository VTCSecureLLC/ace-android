package org.linphone.vtcsecure;


import android.net.Uri;

import com.google.analytics.tracking.android.GoogleAnalytics;

import java.util.HashMap;

/**
 * Created by Patrick on 3/2/16.
 *
 * This class is used to store global variables, for quick easy access, and to keep them organized
 */
public class g {
    public static HashMap<String, Uri> domain_image_hash=new HashMap<String, Uri>();
    public static GAUtils analytics_tracker;
    public static GoogleAnalytics mGaInstance;

    //Variables used for incall, when app is paused or destroyed
    //This would be handled by onSavedInstanceState, but doesn't work when the user chooses to exit
    //the app manually.
    //
    //https://sites.google.com/site/jalcomputing/home/mac-osx-android-programming-tutorial/saving-instance-state

    public static boolean in_call_activity_suspended=false;
    public static boolean isRTTMaximized;
    public static boolean Mic;
    public static boolean AudioMuted;
    public static boolean VideoCallPaused;

    public static boolean intial_call_update_sent=false;


}
