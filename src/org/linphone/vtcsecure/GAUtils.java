
package org.linphone.vtcsecure;

import android.content.Context;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;


public class GAUtils {
    public static GAUtils gaUtils;



    private Tracker mGaTracker;

    public GAUtils(Context context){
        // Get the GoogleAnalytics singleton. Note that the SDK uses
        // the application context to avoid leaking the current context.
        g.mGaInstance = GoogleAnalytics.getInstance(context);

        // Use the GoogleAnalytics singleton to get a Tracker.
        mGaTracker = g.mGaInstance.getTracker("UA-76139509-1"); // Placeholder tracking ID.

    }

    public static GAUtils getInstance(Context context) {
        if (gaUtils == null) {
            synchronized (GAUtils.class) {
                gaUtils = new GAUtils(context);
            }
        }
        return gaUtils;
    }


    public void send(Context context, String category, String action,
                        String desc, Long value) {
        if (null == mGaTracker) {
           // initTracker(context);
        }
        mGaTracker.send(MapBuilder.createEvent(category, action, desc, value)
                .build());
    }

    public void setScreenName(String screenName){
        // Values set directly on a tracker apply to all subsequent hits.
        mGaTracker.set(Fields.SCREEN_NAME, screenName);

        // This screenview hit will include the screen name "Home Screen".
        mGaTracker.send(MapBuilder.createAppView().build());
    }
}
