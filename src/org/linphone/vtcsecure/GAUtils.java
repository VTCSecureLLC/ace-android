
package org.linphone.vtcsecure;

import android.content.Context;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;


public class GAUtils {
    public static GAUtils gaUtils;
    public static EasyTracker tracker;

    public GAUtils(){

    }

    public static GAUtils getInstance() {
        if (gaUtils == null) {
            synchronized (GAUtils.class) {
                gaUtils = new GAUtils();
            }
        }
        return gaUtils;
    }

    public EasyTracker initTracker(Context context) {
        GoogleAnalytics.getInstance(context).getTracker("UA-76139509-1");
        tracker = EasyTracker.getInstance(context);
        return tracker;
    }

    public void send(Context context, String category, String action,
                        String desc, Long value) {
        if (null == tracker) {
            initTracker(context);
        }
        tracker.send(MapBuilder.createEvent(category, action, desc, value)
                .build());
    }

    public void setScreenName(String screenName){
        // Values set directly on a tracker apply to all subsequent hits.
        tracker.set(Fields.SCREEN_NAME, screenName);

        // This screenview hit will include the screen name "Home Screen".
        tracker.send(MapBuilder.createAppView().build());
    }
}
