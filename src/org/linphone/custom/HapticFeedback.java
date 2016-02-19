package org.linphone.custom;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;


/**
 * Created by Vardan on 2/15/2016.
 */



public class HapticFeedback {
    private static final long DURATION = 10;  // millisec.
    private static final int DTMF_DURATION = 150;

    private static final int NO_REPEAT = -1;
    private static final String TAG = "HapticFeedback";
    private Context mContext;
    private long[] mHapticPattern;
    private Vibrator mVibrator;
    private boolean mEnabled;
    private Settings.System mSystemSettings;
    private ContentResolver mContentResolver;
    private boolean mSettingEnabled;
    private boolean mDtmfSoundEnabled;


    ToneGenerator mToneGenerator;
    private final Object mToneGeneratorLock = new Object();
    private boolean mVibrateEnabled;


    public void init(Context context, boolean enabled) {
        mContext = context;
        mEnabled = enabled;
        if (enabled) {

            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            mHapticPattern = new long[] {0, DURATION, 2 * DURATION, 3 * DURATION};
            mSystemSettings = new Settings.System();
            mContentResolver = context.getContentResolver();

            initTone();
        }
    }

    public void deInit()
    {
        stopTone();
        releaseTone();
    }


    public void checkSystemSetting() {
        if (!mEnabled) {
            return;
        }
        try {
            int val = Settings.System.getInt(mContentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
            mSettingEnabled = val != 0;
        } catch (Resources.NotFoundException nfe) {
            Log.e(TAG, "Could not retrieve system setting.", nfe);
            mSettingEnabled = false;
        }

        // uncomment if we want to ignore system params
        //mSettingEnabled = true;
    }

    public void onEvent(int key)
    {
        vibrate();
        dtmfPlay(key);
    }


    private void dtmfPlay(int tone)
    {

        playTone(tone, DTMF_DURATION);

    }
    private void playTone(int tone, int durationMs)
    {
        if(!mDtmfSoundEnabled || mContext == null)
            return;



        AudioManager audioManager =
                (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
                || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }
        synchronized (mToneGenerator) {
            if (mToneGenerator == null) {
                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
                return;
            }
            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, durationMs);
        }
    }
    private void stopTone()
    {
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "stopTone: mToneGenerator == null");
                return;
            }
            mToneGenerator.stopTone();
        }
    }
    private void releaseTone()
    {
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
    }
    private void initTone()
    {
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    // VOLUME is hardcoded to 100
                    mToneGenerator = new ToneGenerator( AudioManager.STREAM_DTMF, 100);
                } catch (RuntimeException e) {
                    Log.w(TAG, "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }
    }


    private void vibrate() {
        if (!mEnabled || !mSettingEnabled || !mVibrateEnabled) {
            return;
        }

        if (mHapticPattern != null && mHapticPattern.length == 1) {
            mVibrator.vibrate(mHapticPattern[0]);
        } else {
            mVibrator.vibrate(mHapticPattern, NO_REPEAT);
        }
    }

    public void setDTMFSoundEnabled(boolean enabled) {
        this.mDtmfSoundEnabled = enabled;
    }

    public void setHapticEnabled(boolean enabled) {
        this.mVibrateEnabled = true;
    }

}