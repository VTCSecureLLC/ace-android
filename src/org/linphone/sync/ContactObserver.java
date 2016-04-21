package org.linphone.sync;

import android.database.ContentObserver;
import android.os.Handler;

import org.linphone.mediastream.Log;

/**
 * Created by accontech-samson on 4/19/16.
 */
public class ContactObserver extends ContentObserver {


    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public ContactObserver(Handler handler) {
        super(handler);
    }

    @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            Log.d("onContact changed");
        }


}
