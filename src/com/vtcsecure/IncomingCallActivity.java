/*
IncomingCallActivity.java
Copyright (C) 2011  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.vtcsecure;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vtcsecure.ace.LinphoneTorchFlasher;
import com.vtcsecure.ui.AvatarWithShadow;
import com.vtcsecure.ui.LinphoneSliders;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity displayed when a call comes in.
 * It should bypass the screen lock mechanism.
 *
 * @author Guillaume Beraudo
 */
public class IncomingCallActivity extends Activity implements LinphoneSliders.LinphoneSliderTriggered {

	private static IncomingCallActivity instance;

	private TextView mNameView;
	private TextView mNumberView;
	private AvatarWithShadow mPictureView;
	private LinphoneCall mCall;
	private LinphoneSliders mIncomingCallWidget;
	private LinphoneCoreListenerBase mListener;
	private RelativeLayout topLayout; 
	private Boolean backgroundIsRed = false;
	private Boolean torhcIsOn = false;
	private Timer flashRedBackgroundTimer;
	private Timer vibrateTimer;
	private boolean terminated = false;
    private int ringCount = 0;

	public static IncomingCallActivity instance() {
		return instance;
	}

	public static boolean isInstanciated() {
		return instance != null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.incoming);

		mNameView = (TextView) findViewById(R.id.incoming_caller_name);
		mNumberView = (TextView) findViewById(R.id.incoming_caller_number);
		mPictureView = (AvatarWithShadow) findViewById(R.id.incoming_picture);
		topLayout = (RelativeLayout)findViewById(R.id.topLayout);

		// set this flag so this activity will stay in front of the keyguard
		int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
		getWindow().addFlags(flags);

		// "Dial-to-answer" widget for incoming calls.
		mIncomingCallWidget = (LinphoneSliders) findViewById(R.id.sliding_widget);
		mIncomingCallWidget.setOnTriggerListener(this);

		mListener = new LinphoneCoreListenerBase(){
			@Override
			public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
				if (call == mCall && State.CallEnd == state) {
					finish();
				}
				if (state == State.StreamsRunning) {
					// The following should not be needed except some devices need it (e.g. Galaxy S).
					LinphoneManager.getLc().enableSpeaker(LinphoneManager.getLc().isSpeakerEnabled());
				}
			}
		};

		super.onCreate(savedInstanceState);
		instance = this;
	}

	@Override
	protected void onResume() {
        super.onResume();
		instance = this;

		// VTCSecure

		flashRedBackground();
		flashTorch();
		vibrate();
        stopRingCount();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.addListener(mListener);
		}

		// Only one call ringing at a time is allowed
		if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
			List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
			for (LinphoneCall call : calls) {
				if (State.IncomingReceived == call.getState()) {
					mCall = call;
					break;
				}
			}
		}
		if (mCall == null) {
			Log.e("Couldn't find incoming call");
			finish();
			return;
		}
		LinphoneAddress address = mCall.getRemoteAddress();
		// May be greatly sped up using a drawable cache
		Contact contact = ContactsManager.getInstance().findContactWithAddress(getContentResolver(), address);
		LinphoneUtils.setImagePictureFromUri(this, mPictureView.getView(), contact != null ? contact.getPhotoUri() : null,
                contact != null ? contact.getThumbnailUri() : null, R.drawable.unknown_small);

		// To be done after findUriPictureOfContactAndSetDisplayName called
		mNameView.setText(contact != null ? contact.getName() : "");
		if (getResources().getBoolean(R.bool.only_display_username_if_unknown)) {
			mNumberView.setText(address.getUserName());
		} else {
			mNumberView.setText(address.asStringUriOnly());
		}
	}

	@Override
	protected void onPause() {
		terminated = true;
		LinphoneTorchFlasher.instance().stopFlashTorch();
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.removeListener(mListener);
		}
        stopRingCount();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (LinphoneManager.isInstanciated() && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
			LinphoneManager.getLc().terminateCall(mCall);
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void flashRedBackground () {
		flashRedBackgroundTimer = new Timer();
		final float flashFrequencyInSeconds = LinphonePreferences.instance().getConfig().getFloat("vtcsecure", "incoming_flashred_frequency", 0.3f);
		flashRedBackgroundTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				IncomingCallActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Integer colorFrom = Color.TRANSPARENT;
						Integer colorTo = Color.rgb(90, 17, 17);//RED;

						AnimatorSet animatorSet = new AnimatorSet();
						ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
						colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(ValueAnimator animator) {
								topLayout.setBackgroundColor((Integer) animator.getAnimatedValue());
							}
						});

						ValueAnimator reverseColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorTo, colorFrom);
						reverseColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(ValueAnimator animator) {
								topLayout.setBackgroundColor((Integer) animator.getAnimatedValue());
							}
						});
						colorAnimation.setDuration((long) (flashFrequencyInSeconds * 3000));
						reverseColorAnimation.setDuration((long) (flashFrequencyInSeconds * 3000));

						if (terminated) {
							flashRedBackgroundTimer.cancel();
						} else {
							animatorSet.play(colorAnimation).after(reverseColorAnimation);
							animatorSet.start();
						}
					}
				});
			}
		}, 0, (long)(flashFrequencyInSeconds*1000));
	}


	private void vibrate() {
		vibrateTimer= new Timer();
		float vibrateFrequencyInSeconds = LinphonePreferences.instance().getConfig().getFloat("vtcsecure", "incoming_vibrate_frequency", 0.3f);
		final Vibrator v = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

		vibrateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				IncomingCallActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (terminated){
                            vibrateTimer.cancel();
                        } else {
                            incrementRingCount();
                            v.vibrate(500);
                        }
					}
				});
			}
		}, 0, (long)(vibrateFrequencyInSeconds*1000));

	}

	private void flashTorch() {
		if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return;
		LinphoneTorchFlasher.instance().startFlashTorch();
	}

	
	private void decline() {
		LinphoneTorchFlasher.instance().stopFlashTorch();
		LinphoneManager.getLc().terminateCall(mCall);
	}

	private void answer() {
		
		LinphoneTorchFlasher.instance().stopFlashTorch();
		LinphoneCallParams params = LinphoneManager.getLc().createDefaultCallParameters();

		boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(this);
		if (isLowBandwidthConnection) {
			params.enableLowBandwidth(true);
			Log.d("Low bandwidth enabled in call params");
		}

		if (!LinphoneManager.getInstance().acceptCallWithParams(mCall, params)) {
			// the above method takes care of Samsung Galaxy S
			Toast.makeText(this, R.string.couldnt_accept_call, Toast.LENGTH_LONG).show();
		} else {
			if (!LinphoneActivity.isInstanciated()) {
				return;
			}
			final LinphoneCallParams remoteParams = mCall.getRemoteParams();
			if (remoteParams != null && remoteParams.getVideoEnabled() && LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests()) {
				LinphoneActivity.instance().startVideoActivity(mCall);
			} else {
				LinphoneActivity.instance().startIncallActivity(mCall);
			}
		}
	}

	@Override
	public void onLeftHandleTriggered() {
		answer();
		finish();
	}

	@Override
	public void onRightHandleTriggered() {
		decline();
		finish();
	}

    private void incrementRingCount() {
        final TextView outgoingRingCountTextView = (TextView)findViewById(R.id.outboundRingCount);
        outgoingRingCountTextView.setVisibility(View.VISIBLE);
        ringCount++;
        outgoingRingCountTextView.setText(ringCount + "");

    }

    private void stopRingCount() {
        ringCount = 0;
        final TextView outgoingRingCountTextView = (TextView)findViewById(R.id.outboundRingCount);
        outgoingRingCountTextView.setVisibility(View.GONE);
        outgoingRingCountTextView.setText(ringCount + "");

    }
}
