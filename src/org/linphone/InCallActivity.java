package org.linphone;
/*
InCallActivity.java
Developed pursuant to contract FCC15C0008 as open source software under GNU General Public License version 2.

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

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphonePlayer;
import org.linphone.core.Reason;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.ui.Numpad;
import org.linphone.vtcsecure.LinphoneTorchFlasher;
import org.linphone.vtcsecure.Utils;
import org.linphone.vtcsecure.g;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import joanbempong.android.HueController;

/**
 * @author Sylvain Berfini
 */
public class InCallActivity extends FragmentActivity implements OnClickListener {
	public boolean animating_show_controls=false;

	public final static int NEVER = -1;
	public final static int NOW = 0;
	public final static int SECONDS_BEFORE_HIDING_CONTROLS = 3000;
	public final static int SECONDS_BEFORE_DENYING_CALL_UPDATE = 30000;

	private static InCallActivity instance;

	ArrayList<String> linphone_core_stats_list;

	private Handler mControlsHandler = new Handler();
	private Runnable mControls;
	private TextView switchCamera;
	private TextView toggleSpeaker;

	private boolean isCameraMutedPref;

	private TextView chat_button, hangUp, addCall, transfer, conference;
	private ImageView video, micro, dialer, audioMute, options, audioRoute;
	private TextView routeSpeaker, routeReceiver, routeBluetooth;
	private LinearLayout routeLayout;
	private boolean isAnimatingHideControllers;

	//TODO: remove
//	private ProgressBar videoProgress;
	private StatusFragment status;
	private VideoCallFragment videoCallFragment;
	private boolean isCameraMutedOnStart=false, isCameraMuted=false, isMicMuted = false, isTransferAllowed, isAnimationDisabled,
			isRTTLocallyEnabled = false, isRTTEnabled=true;
	private boolean isAudioMuted;
	private boolean isSpeakerOn;
	public ViewGroup mControlsLayout;
	public LinearLayout mIncomingcallsLayout;
	private View acceptBtn, declineBtn, callLaterBtn;
	private Numpad numpad;
	private int cameraNumber;
	private Animation slideOutLeftToRight, slideInRightToLeft, slideInBottomToTop, slideInTopToBottom, slideOutBottomToTop, slideOutTopToBottom;
	private CountDownTimer timer;
	private boolean isVideoCallPaused = false;
	AcceptCallUpdateDialogFragment callUpdateDialog;
	Typeface rtt_typeface;

	private LayoutInflater inflater;
	private ViewGroup container;
	private boolean isConferenceRunning = false;
	private boolean showCallListInVideo = false;
	private LinphoneCoreListenerBase mListener;
	private Timer outgoingRingCountTimer = null;

	public Contact contact;
	boolean isEmergencyCall;
	private RelativeLayout statusContainer;
	private Timer flashRedBackgroundTimer;

	// RTT views
	private int TEXT_MODE;
	private int NO_TEXT=-1;
	private int RTT=0;
	private int SIP_SIMPLE=1;
	private TextWatcher rttTextWatcher;
	private ScrollView rtt_scrollview;
	private View rttContainerView;
	private View rttHolder;

	String contactName = "";

	int OUTGOING=0;
	int INCOMING=1;

	private boolean isRTTMaximized = false;
	private boolean isIncomingBubbleCreated = false;
	public int rttIncomingBubbleCount=0;
	private int rttOutgoingBubbleCount=0;
	public boolean incoming_chat_initiated=false;
	private SharedPreferences prefs;
	private EditText previousoutgoingEditText;
	private EditText outgoingEditText;
	private TextView incomingTextView;
	View mFragmentHolder;
	View mViewsHolder;
	View linphone_core_stats_holder;
	TableLayout linphone_core_stats_table;
	RelativeLayout mainLayout;
	final float mute_db = -1000.0f;

	private boolean isFlashing;
	private ImageView mIncomingImage, mPassiveImage;
	private TextView mIncomingUserName, mIncomingCallType, mIncomingCallCount;
	private TextView mIncomingPassiveUserName;
	private Chronometer mIncomingPassiveCallHoldTime;
	private int mRingCount;

	private RelativeLayout mInComingCallHeader;
	private RelativeLayout mInPassiveCallHeader;

	Handler mHandler;

	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
	private int field = 0x00000020;

	private HeadPhoneJackIntentReceiver myReceiver;
	private boolean finishWithoutDelay;
	private TextView tv_sub_status;
	private TextView outboundRingCountView;
	private TextView labelRingingView;

	public static InCallActivity instance() {
		return instance;
	}

	public static boolean isInstanciated() {
		return instance != null;
	}

	private void invalidateStatusContainer(){
		boolean res= tv_sub_status.getVisibility() == View.GONE && outboundRingCountView.getVisibility() == View.GONE && labelRingingView.getVisibility() == View.GONE;
		if(res)
			statusContainer.setVisibility(View.GONE);
		else
			statusContainer.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("ttt onCreate()");
		mHandler = new Handler();

		try {
			// Yeah, this is hidden field.
			field = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
		} catch (Throwable ignored) {
		}

		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(field, getLocalClassName());

		instance = this;


		LinphoneService.instance().setActivityToLaunchOnIncomingReceived(InCallActivity.class);

		//DialerFragment.instance().mOrientationHelper.disable();

		LinphoneActivity.instance().mOrientationHelper.enable();

		LinphoneCore lc = LinphoneManager.getLc();
		LinphoneCall currentCall = lc.getCurrentCall();
		if (currentCall != null) {
			isEmergencyCall = CallManager.getInstance().isEmergencyCall(currentCall.getRemoteAddress());
			if (currentCall.getState() ==State.StreamsRunning)
				finishWithoutDelay = true;
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		mainLayout = new RelativeLayout(this);
		mainLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		LayoutInflater inflator = LayoutInflater.from(this);
		mViewsHolder =  inflator.inflate(R.layout.new_incall, null);

		mFragmentHolder = inflator.inflate(R.layout.incall_fragment_holder, null);
		if(LinphonePreferences.instance().isForce508()){
			rttHolder =  inflator.inflate(R.layout.rtt_holder_508, null);
		}else{
			rttHolder =  inflator.inflate(R.layout.rtt_holder, null);
		}

		handleNotificationMessage();
		View statusBar = inflator.inflate(R.layout.status_holder, null);
		RelativeLayout.LayoutParams paramss = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);



		linphone_core_stats_holder =  inflator.inflate(R.layout.linphone_core_stats, null);
		linphone_core_stats_table = (TableLayout)linphone_core_stats_holder.findViewById(R.id.linphone_core_stats);
		show_extra_linphone_core_stats();

		paramss.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//		mFragmentHolder.setVisibility(View.GONE);
		mainLayout.addView(mFragmentHolder, paramss);
//
//		mViewsHolder.setVisibility(View.GONE);
//		rttHolder.setVisibility(View.GONE);
//		linphone_core_stats_table.setVisibility(View.GONE);
		mainLayout.addView(mViewsHolder);
		mainLayout.addView(rttHolder, paramss);
		//
		inflator.inflate(R.layout.incoming_call_controllers_container, mainLayout, true );
		mainLayout.addView(statusBar);
		mainLayout.addView(linphone_core_stats_holder, paramss);


//		View myView = inflator.inflate(R.layout.test, null);
//		mainLayout.addView(myView);
		setContentView(mainLayout);
		initShowErrorView();
		initUI();

		myReceiver = new HeadPhoneJackIntentReceiver();

		isTransferAllowed = getApplicationContext().getResources().getBoolean(R.bool.allow_transfers);
		showCallListInVideo = getApplicationContext().getResources().getBoolean(R.bool.show_current_calls_above_video);
		LinphoneManager.getLc().enableSpeaker(true);


		//if (params.realTimeTextEnabled()) { // Does not work, always false
		isRTTLocallyEnabled=LinphoneManager.getInstance().getRttPreference();

		isAnimationDisabled = getApplicationContext().getResources().getBoolean(R.bool.disable_animations) || !LinphonePreferences.instance().areAnimationsEnabled();
		cameraNumber = AndroidCameraConfiguration.retrieveCameras().length;

		getTextMode();

		isCameraMuted = prefs.getBoolean(getString(R.string.pref_av_camera_mute_key), false);

		isMicMuted = prefs.getBoolean(getString(R.string.pref_av_mute_mic_key), false);
		if(isEmergencyCall) {
			isMicMuted = false;
			isCameraMuted = false;
		}

		LinphoneManager.getLc().muteMic(isMicMuted);

		micro.setSelected(isMicMuted);

		isAudioMuted = prefs.getBoolean(getString(R.string.pref_av_speaker_mute_key), false);
		if(isAudioMuted)
		{
			LinphoneManager.getLc().setPlaybackGain(mute_db);
		}

		//set speaker on initially. This does not mean that audio isn't muted. If audio is muted, and speaker is on, there will still be no sound! Until audio is unmutted
		isSpeakerOn = true;


		status.callStats.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d("stats clicked");
				linphone_core_stats_holder.setVisibility(View.VISIBLE);
			}
		});
		linphone_core_stats_table.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d("stats clicked");
				linphone_core_stats_holder.setVisibility(View.GONE);
			}
		});


		mListener = new LinphoneCoreListenerBase(){
			@Override
			public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom cr) {
				super.isComposingReceived(lc, cr);
				Log.d("RTT incall", "isComposingReceived cr=" + cr.toString());
				Log.d("RTT incall","isRTTMaximaized"+isRTTMaximized);
				Log.d("RTT", "incoming_chat_initiated" + incoming_chat_initiated);

				try {
					if (!cr.isRemoteComposing()) {
						Log.d("RTT incall: remote is not composing, getChar() returns: " + cr.getChar());
						return;
					}
				}catch(Throwable e){

				}
			}

//			@Override
//			public void infoReceived(LinphoneCore lc, LinphoneCall call,
//									 LinphoneInfoMessage info) {
//				Log.d("info received"+info.getHeader("action"));
//				if(info.getHeader("action").equals("camera_mute_off")){
//					VideoCallFragment.cameraCover.setImageResource(R.drawable.camera_mute);
//					VideoCallFragment.cameraCover.setVisibility(View.VISIBLE);
//
//				}else if(info.getHeader("action").equals("camera_mute_on")){
//					VideoCallFragment.cameraCover.setVisibility(View.GONE);
//
//				}
//			}

			@Override
			public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
				super.messageReceived(lc, cr, message);
				Log.d("RTT", "messageReceived cr=" + message.toString());
//				if(message.toString().startsWith("@@info@@ "))
//				{
//					labelRingingView.setVisibility(View.VISIBLE);
//					tv_sub_status.setVisibility(View.VISIBLE);
//
//					labelRingingView.setText(getResources().getString(R.string.call_declined_with_the_message));
//					tv_sub_status.setText(message.getText().replace("@@info@@ ", ""));
////					startStatusFlashingAndFinish();
//
//					tv_sub_status.setVisibility(View.VISIBLE);
//					invalidateStatusContainer();
//				}

			}

			@Override
			public void callState(LinphoneCore lc, final LinphoneCall call, LinphoneCall.State state, String message) {
				Log.d("TAG", "callState change");
				try {
					LinphoneActivity.instance().display_all_core_values(lc, state.toString());
				}catch(Throwable e){
					e.printStackTrace();
				}

				if(state == State.OutgoingRinging) {
					String ringingText = "Ringing... \n" + call.getRemoteAddress().asStringUriOnly();
					labelRingingView.setLines(4);
					labelRingingView.setText(ringingText);
				}

				if(state == State.CallEnd || state == State.Error || state == State.CallReleased
						|| state == State.Connected
						|| state == State.StreamsRunning){

					if((state == State.CallEnd || state == State.Error)) {
						if (call.getErrorInfo() != null && call.getDirection() == CallDirection.Outgoing) {
							String call_end_reason = Utils.getReasonText(call.getErrorInfo().getReason(), InCallActivity.this);
							tv_sub_status.setText(call_end_reason);
							tv_sub_status.setVisibility(View.VISIBLE);
							invalidateStatusContainer();
						}
					}

					try {
						labelRingingView.setVisibility(View.GONE);
						outboundRingCountView.setVisibility(View.GONE);
						invalidateStatusContainer();
					}
					catch (Exception ex)
					{

					}
					stopOutgoingRingCount();

				}

				if (lc.getCallsNb() == 0) {
					finishWithDelay();
					stopOutgoingRingCount();
					return;
				}

				if (lc.getCallsNb() == 1 && lc.getCalls()[0].getState() == State.Paused) {
					if(state == State.CallEnd)
						checkIncomingCall();
					lc.resumeCall(lc.getCalls()[0]);

					return;
				}
				if(state==State.IncomingReceived||state == State.OutgoingInit) {
					if(LinphoneManager.getLc().getCallsNb() > 2){
						for(LinphoneCall mCall: LinphoneManager.getLc().getCalls()){
							if(mCall.getState() == State.IncomingReceived){
								LinphoneManager.getLc().declineCall(mCall, Reason.Busy);
							}
						}
					}

					LinphoneManager.getInstance().initSDP(isVideoEnabled(call));
				}
				if (state == State.IncomingReceived || State.CallEnd == state) {
					//startIncomingCallActivity();
					checkIncomingCall();

					return;
				}


				if (state == State.Paused || state == State.PausedByRemote ||  state == State.Pausing) {
					video.setEnabled(false);
					if(!isVideoEnabled(call)){
						showAudioView();
					}

				}

				if (state == State.Resuming) {
					if(LinphonePreferences.instance().isVideoEnabled()){
						status.refreshStatusItems(call, isVideoEnabled(call));
						if(isVideoEnabled(call)){
							showVideoView();
						}
					}
				}

				if (state == State.StreamsRunning) {
					if(isCameraMuted)
						setCameraMute(isCameraMuted);
					finishWithoutDelay = true;

					if(VideoCallFragment.mCaptureView != null)
						invalidateSelfView(VideoCallFragment.mCaptureView);
					if(isRTTLocallyEnabled) {
						isRTTEnabled = call.getRemoteParams().realTimeTextEnabled();
					}
					else{
						isRTTEnabled = false;
					}

					switchVideo(isVideoEnabled(call));
					//Check media in progress
					if(LinphonePreferences.instance().isVideoEnabled() && !call.mediaInProgress()){
						video.setEnabled(true);
					}
					isMicMuted = lc.isMicMuted();
					isAudioMuted = lc.getPlaybackGain() == mute_db;

					enableAndRefreshInCallActions();

					if (status != null) {
						//TODO: remove
//						videoProgress.setVisibility(View.GONE);
						status.refreshStatusItems(call, isVideoEnabled(call));
					}
					statusContainer.setVisibility(View.GONE);
				}

				refreshInCallActions();

				refreshCallList(getResources());

				if (state == State.CallUpdatedByRemote) {
					// If the correspondent proposes video while audio call
					boolean videoEnabled = LinphonePreferences.instance().isVideoEnabled();
					if (!videoEnabled) {
						acceptCallUpdate(false);
						return;
					}


					boolean remoteVideo = call.getRemoteParams().getVideoEnabled();
					boolean localVideo = call.getCurrentParamsCopy().getVideoEnabled();
					boolean autoAcceptCameraPolicy = LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests();
					if (remoteVideo && !localVideo && !autoAcceptCameraPolicy && !LinphoneManager.getLc().isInConference()) {
						showAcceptCallUpdateDialog();

						timer = new CountDownTimer(SECONDS_BEFORE_DENYING_CALL_UPDATE, 1000) {
							public void onTick(long millisUntilFinished) { }
							public void onFinish() {
								if (callUpdateDialog != null)
									callUpdateDialog.dismiss();
								acceptCallUpdate(false);
							}
						}.start();
					}
//        			else if (remoteVideo && !LinphoneManager.getLc().isInConference() && autoAcceptCameraPolicy) {
//        				mHandler.post(new Runnable() {
//        					@Override
//        					public void run() {
//        						acceptCallUpdate(true);
//        					}
//        				});
//        			}
				}
				transfer.setEnabled(LinphoneManager.getLc().getCurrentCall() != null);
			}

			@Override
			public void callEncryptionChanged(LinphoneCore lc, final LinphoneCall call, boolean encrypted, String authenticationToken) {
				if (status != null) {
					status.refreshStatusItems(call, call.getCurrentParamsCopy().getVideoEnabled());
				}
			}
		};


		if (findViewById(R.id.fragmentContainer) != null) {
//			initUI();

			if (LinphoneManager.getLc().getCallsNb() > 0) {
				LinphoneCall call = LinphoneManager.getLc().getCalls()[0];

				if (LinphoneUtils.isCallEstablished(call)) {
					enableAndRefreshInCallActions();
				}
			}

			if (savedInstanceState != null) {
				Log.d("getting savedInstanceState");
				// Fragment already created, no need to create it again (else it will generate a memory leak with duplicated fragments)
				isRTTMaximized = savedInstanceState.getBoolean("isRTTMaximized");
				isMicMuted = savedInstanceState.getBoolean("Mic");
				isAudioMuted = savedInstanceState.getBoolean("AudioMuted");
				isVideoCallPaused = savedInstanceState.getBoolean("VideoCallPaused");
				refreshInCallActions();
				return;
			} else if(g.in_call_activity_suspended){//This happens when app is destroyed and savedInstanceState was not run. (When the user physically exits the app, then returns.)
				isRTTMaximized = g.isRTTMaximized;
				isMicMuted = g.Mic;
				isAudioMuted = g.AudioMuted;
				isVideoCallPaused = g.VideoCallPaused;
				refreshInCallActions();
			}

			Fragment callFragment = null;
			LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
			callFragment = new VideoCallFragment();
			videoCallFragment = (VideoCallFragment) callFragment;

			if(call != null && call.getDirection() == CallDirection.Outgoing && isConnecting(call.getState())){
				if(call.getState() == State.OutgoingRinging) {
					String ringingText = "Ringing... \n" + call.getRemoteAddress().asStringUriOnly();
					labelRingingView.setLines(4);
					labelRingingView.setText(ringingText);
				}
				else{
					String connectingText = labelRingingView.getText() + "\n" + call.getRemoteAddress().asStringUriOnly();
					labelRingingView.setText(connectingText);
				}
				startOutgoingRingCount();
			}

			if(BluetoothManager.getInstance().isBluetoothHeadsetAvailable()){
				BluetoothManager.getInstance().routeAudioToBluetooth();
			}

			callFragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, callFragment).commitAllowingStateLoss();


			if(call != null) {
				LinphoneCallParams params = call.getCurrentParamsCopy();
				initRTT();

				if (isRTTMaximized) {
					showRTTinterface();
				}
			}

			if (call != null) {
				if (call.getState() == State.StreamsRunning) {
					setCameraMute(isCameraMuted);
				}
			}

		}

		invalidateStatusContainer();
		checkIncomingCall();




	}

	private boolean isConnecting(State state)
	{
		Log.d("isConnecting() state:" + state);
		boolean res = false;
		if(state == State.OutgoingEarlyMedia || state == State.OutgoingInit || state == State.OutgoingProgress  || state == State.OutgoingRinging)
			res = true;
		return res;
	}

	private void handleNotificationMessage() {
		if(!(  getIntent()!= null && getIntent().hasExtra("GoToChat") && getIntent().hasExtra("ChatContactSipUri") && LinphoneActivity.instance() != null ))
			return;
		String url = getIntent().getExtras().getString("ChatContactSipUri");

		if(LinphoneManager.getLc().getCallsNb() == 0)
			LinphoneActivity.instance().showMessageFromNotification(getIntent());
		else if(LinphoneManager.getLc().getCallsNb() == 1 && rttHolder!= null && rttHolder.getVisibility() != View.VISIBLE){
			showRTTinterface();
		}
	}
	public void invalidateSelfView(SurfaceView sv) {

		LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if(call == null)
			return;

		LinphoneCallParams params = call.getCurrentParamsCopy();
		int sent_video_height = params.getSentVideoSize().height;
		int sent_video_width = params.getSentVideoSize().width;

		if(sent_video_height == 0 || sent_video_width == 0)
		{
			return;
		}
		float ratio = sent_video_height / (float) sent_video_width;
		if(sv!= null)
		{
			ViewGroup.LayoutParams layoutParams = sv.getLayoutParams();
			float currentratio = layoutParams.height / (float) layoutParams.width;
			float diff = currentratio / ratio;
			Log.d("difference in selfview = " + diff + "  send widthxheight " + sent_video_width+ "X" + sent_video_height);
			if (diff>1.02f || diff < 0.98f)
			{
				layoutParams.height = (int) (ratio * layoutParams.width);
				sv.setLayoutParams(layoutParams);
			}
		}
		else
		{
			Log.d("difference in selfview NULLLLLL");
		}

	}

	private void initIncomingCallsViews() {
		mIncomingUserName = (TextView) findViewById(R.id.label_incoming_call_user_name);
		mIncomingCallType = (TextView) findViewById(R.id.label_incoming_call_type);
		mIncomingCallCount = (TextView) findViewById(R.id.label_incoming_call_count);
		mIncomingPassiveUserName = (TextView) findViewById(R.id.label_incoming_call_passive_user_name);
		mIncomingPassiveCallHoldTime = (Chronometer) findViewById(R.id.label_incoming_call_hold_time);
		mIncomingImage =(ImageView) findViewById(R.id.imageview_incoming_call_user_image);
		mPassiveImage = (ImageView) findViewById(R.id.imageview_incoming_call_passive_user_image);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d("onConfigChanged");

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (VideoCallFragment.mCaptureView != null) {
				ViewGroup.LayoutParams params = VideoCallFragment.mCaptureView.getLayoutParams();
				int width = params.width;
				int height = params.height;
				if (params.width < params.height) {
					params.height = width;
					params.width = height;
					VideoCallFragment.mCaptureView.setLayoutParams(params);
				}
			}
		}
		else
		{
			if (VideoCallFragment.mCaptureView != null) {
				ViewGroup.LayoutParams params = VideoCallFragment.mCaptureView.getLayoutParams();
				int width = params.width;
				int height = params.height;
				if (params.width > params.height) {
					params.height = width;
					params.width = height;
					VideoCallFragment.mCaptureView.setLayoutParams(params);
				}

			}
		}



		boolean contralersVisible = mControlsLayout.getVisibility() == View.VISIBLE;
//		mainLayout.removeView(mViewsHolder);
//
//
//		mViewsHolder = (ViewGroup) getLayoutInflater().inflate(R.layout.new_incall, null);
//
//		mainLayout.addView(mViewsHolder, 1);
		initShowErrorView();
		initUI();
		if(!contralersVisible)
			mControlsLayout.setVisibility(View.INVISIBLE);

		/*if(isRTTEnabled){
			initRTT();
		}*/
		/*if(isRTTMaximized){
			showRTTinterface();
		}
		if (isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
			displayVideoCallControlsIfHidden();
		}
*/
		if (LinphoneManager.getLc().getCallsNb() > 0) {
			LinphoneCall call = LinphoneManager.getLc().getCalls()[0];

			if (LinphoneUtils.isCallEstablished(call)) {
				enableAndRefreshInCallActions();
			}
		}
		//refreshCallList(getResources());
		//handleViewIntent();
		checkIncomingCall();

	}

	public void getTextMode(){
		prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());
		Log.d("Text Send Mode" + prefs.getString(getString(R.string.pref_text_settings_send_mode_key), "RTT"));
		String text_mode=prefs.getString(getString(R.string.pref_text_settings_send_mode_key), "RTT");
		if(text_mode.equals("SIP_SIMPLE")) {
			TEXT_MODE=SIP_SIMPLE;
		}else if (text_mode.equals("RTT")) {
			TEXT_MODE = RTT;

		}
		Log.d("TEXT_MODE ", TEXT_MODE);

		String font_family = prefs.getString(getString(R.string.pref_text_settings_font_key), "Default");

		String style = prefs.getString(getString(R.string.pref_text_settings_font_style_key), "Default");

		int font_style = Typeface.NORMAL;

		if (style.equals("Default")) {
			font_style = Typeface.NORMAL;
		} else if (style.equals("Bold")){
			font_style = Typeface.BOLD;
		}else if (style.equals("Italic")){
			font_style = Typeface.ITALIC;
		}else if (style.equals("Bold Italic")){
			font_style = Typeface.BOLD_ITALIC;
		}

		if(!font_family.equals("Default"))
		{
			rtt_typeface = Typeface.create(font_family, font_style);
			Log.d("RTT FONT FAMILY: " + font_family + " FONT STYLE: " + font_style);
		}
		else if(font_style != Typeface.NORMAL)
		{
			rtt_typeface = Typeface.defaultFromStyle(font_style);
		}
	}

	public void hold_cursor_at_end_of_edit_text(final EditText et) {
		et.setCursorVisible(false);
		et.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				et.setSelection(et.getText().length());
			}
		});
	}


	public void show_extra_linphone_core_stats(){
		//Add all linphone core stats.
		linphone_core_stats_list=LinphoneActivity.instance().display_all_core_values(LinphoneManager.getLc(), "In Call Stats Populated");
		for(int i=0; i<linphone_core_stats_list.size(); i++){

			TableRow tr=new TableRow(LinphoneActivity.instance());
			tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

			TextView label=new TextView(LinphoneActivity.instance());
			label.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
			String label_string=linphone_core_stats_list.get(i).split(",")[0];
			label.setText(label_string);
			label.setTextColor(Color.WHITE);
			label.setTextSize(12);
			tr.addView(label);

			TextView content=new TextView(LinphoneActivity.instance());
			content.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
			content.setText(linphone_core_stats_list.get(i).subSequence(label_string.length()+1, linphone_core_stats_list.get(i).length()));
			content.setTextColor(Color.WHITE);
			content.setTextSize(12);
			tr.addView(content);

			linphone_core_stats_table.addView(tr);

		}
		//callStats.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 5000));
		//callStats.invalidate();
		//((ScrollView)callStats.getParent()).invalidate();
	}



	/** Initializes the views and other components needed for RTT in a call */
	private void initRTT(){
		rttContainerView = findViewById(R.id.rtt_container);

		rtt_scrollview = (ScrollView)findViewById(R.id.rtt_scrollview);
		rtt_scrollview.getChildAt(0).setOnClickListener(this);

		rttTextWatcher = new TextWatcher() {
			boolean enter_pressed;
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {


				enter_pressed = s.length() > 0 && s.subSequence(s.length() - 1, s.length()).toString().equalsIgnoreCase("\n");
				int text_len = outgoingEditText.getText().toString().replace("\n", "").length();


				if(text_len==0)
				{
					outgoingEditText.setBackgroundResource(0);
				}
				else if(text_len==1)
				{
					if(LinphonePreferences.instance().isForce508()){
						outgoingEditText.setBackgroundResource(R.drawable.chat_bubble_outgoing_508);
					}else{
						outgoingEditText.setBackgroundResource(R.drawable.chat_bubble_outgoing);
					}
					standardize_bubble_view(outboundRingCountView);
				}
				if(text_len == 0)
				{
					previousoutgoingEditText=outgoingEditText;
					return;
				}

				char enter_button=(char) 10;
				char back_space_button=(char) 8;

				if(TEXT_MODE==RTT){
					if(enter_pressed){

						previousoutgoingEditText=outgoingEditText;
						sendRttCharacter(enter_button);
						create_new_outgoing_bubble(outgoingEditText, /*true*/ true);
						outgoingEditText.setBackgroundResource(0);
					}else if(count > before){

						CharSequence last_letter_of_sequence = s.subSequence(start + before, start + count);
						Log.d("last_letter_of_sequence="+last_letter_of_sequence);

						int numeric_value=Character.getNumericValue(last_letter_of_sequence.charAt(0));
						Log.d("numeric value="+numeric_value);

						sendRttCharacterSequence(last_letter_of_sequence);
					}else if(count < before){
						sendRttCharacter(back_space_button); // backspace);
					}
				}else if(TEXT_MODE==SIP_SIMPLE){
					previousoutgoingEditText=outgoingEditText;
					if(enter_pressed) {
						//send preceding new line character to force other end to drop a line.
						if(rttOutgoingBubbleCount>1){
							sendRttCharacterSequence("\n"+String.valueOf(s.subSequence(0,s.length()-1)));
						}else{
							sendRttCharacterSequence(String.valueOf(s.subSequence(0,s.length()-1)));
						}
						create_new_outgoing_bubble(outgoingEditText, true);
					}
				}

			}

			@Override
			public void afterTextChanged(Editable s) {
				//REMOVE EXTRA LINE FROM ENTER PRESS
				if(enter_pressed) {
					previousoutgoingEditText.removeTextChangedListener(rttTextWatcher);
					previousoutgoingEditText.setText(previousoutgoingEditText.getText().toString().subSequence(0,previousoutgoingEditText.getText().toString().length()-1));
					previousoutgoingEditText.addTextChangedListener(rttTextWatcher);
				}
			}
		};

		outgoingEditText = (EditText) findViewById(R.id.et_outgoing_bubble);
		outgoingEditText.addTextChangedListener(rttTextWatcher);
		standardize_bubble_view(outgoingEditText);
		hold_cursor_at_end_of_edit_text(outgoingEditText);
		outgoingEditText.setMovementMethod(null);


		try {
			populate_messages();
		}catch(Throwable e){
			//No messages to populate
			e.printStackTrace();
		}

	}

	public int to_dp(int dp){
		final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
		int pixels = (int) (dp * scale + 0.5f);
		return pixels;
	}

	public void disable_bubble_editing(EditText et){
		et.setKeyListener(null);
	}
	public void standardize_bubble_view(TextView tv){
		tv.setSingleLine(false);
		//tv.setPadding(to_dp(10), to_dp(5), to_dp(10), to_dp(20));
		tv.setTextAppearance(this, R.style.RttTextStyle);
		//Log.d("RTT textsize by default="+tv.getTextSize());
		//Default TextSize is 32dp
		tv.setTextSize(16);
		if(!LinphonePreferences.instance().isForce508()){//use transparency if not 508
			if(tv.getBackground()!=null)
				tv.getBackground().setAlpha(180);
		}
		if(rtt_typeface!=null) {
			tv.setTypeface(rtt_typeface);
		}

	}
	public TextView create_new_outgoing_bubble(EditText old_bubble, boolean is_current_editable_bubble){
		/*if(old_bubble!=null){
			disable_bubble_editing(old_bubble);
		}*/
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(to_dp(300), LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.setMargins(to_dp(10), 0, 0, 0);

		TextView et=new TextView(this);
		et.setLayoutParams(lp);
		if(LinphonePreferences.instance().isForce508()){
			et.setBackgroundResource(R.drawable.chat_bubble_outgoing_508);
		}else{
			et.setBackgroundResource(R.drawable.chat_bubble_outgoing);
		}
		et.setTag(true);
		et.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				outgoingEditText.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

			}
		});
		standardize_bubble_view(et);

		//if(TEXT_MODE==RTT) {
//		if(is_current_editable_bubble) {
//			et.addTextChangedListener(rttTextWatcher);
//		}
		//}

//		et.setOnKeyListener(new View.OnKeyListener() { //FIXME: not triggered for software keyboards
//			@Override
//			public boolean onKey(View v, int keyCode, KeyEvent event) {
//				if (event.getAction() == KeyEvent.ACTION_DOWN) {
//					if (keyCode == KeyEvent.KEYCODE_ENTER) {
//						Log.d("ENTER BUTTON PRESSED");
//						if(TEXT_MODE==RTT){
//							sendRttCharacter((char) 10);
//							create_new_outgoing_bubble((EditText) v);
//						}else if(TEXT_MODE==SIP_SIMPLE){
//							String current_message=((EditText) v).getText().toString();
//							sendRttCharacterSequence(current_message+(char) 10);
//							create_new_outgoing_bubble((EditText) v);
//						}
//
//
//					}
//				}
//				return false;
//			}
//		});
		//hold_cursor_at_end_of_edit_text(et);
		//outgoingEditText=et;
		if(((LinearLayout) rttContainerView).getChildCount()==0 || !isIncomingBubbleCreated || !is_current_editable_bubble)
			((LinearLayout) rttContainerView).addView(et);
		else
			((LinearLayout) rttContainerView).addView(et,((LinearLayout) rttContainerView).getChildCount()-1 );

//		et.requestFocus();
//		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.showSoftInput(et, InputMethodManager.SHOW_FORCED);
//
		rtt_scrollview.post(new Runnable() {
			@Override
			public void run() {
				rtt_scrollview.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		rttOutgoingBubbleCount++;
		et.setText(outgoingEditText.getText().toString().replace("\n", ""));
		outgoingEditText.setText("");
		rtt_scrollview.post(new Runnable() {
			@Override
			public void run() {
				rtt_scrollview.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		return et;
	}
	public void updateIncomingTextView(final long character) {
		runOnUiThread(new Runnable(){
			public void run() {
				if(rttHolder.getVisibility()!=View.VISIBLE){
					showRTTinterface();
				}
				if(mControlsLayout.getVisibility()!= View.GONE)
					hide_controls(0);

				if(!incoming_chat_initiated){
					incomingTextView=create_new_incoming_bubble();
					incoming_chat_initiated=true;
				}

				if (incomingTextView == null) return;

//				if(!incomingTextView.isShown()){
//					incomingTextView=create_new_incoming_bubble();
//				}

				String currentText = incomingTextView.getText().toString();
				if (character == 8) {// backspace
					incomingTextView.setText(currentText.substring(0, currentText.length() - 1));
				} else if (character == (long)0x2028) {
					Log.d("RTT: received Line Separator");
					create_new_incoming_bubble();
				} else if (character == 10) {
					if(incomingTextView.getText().length() == 0)
						return;
					Log.d("RTT: received newline");
					if(incomingTextView.getText().length() == 0)
						return;
					incomingTextView.append(System.getProperty("line.separator"));
					create_new_incoming_bubble();
				} else { // regular character
					if(rttIncomingBubbleCount==0){
						Log.d("There was no incoming bubble to send text to, so now we must make one.");
						incomingTextView=create_new_incoming_bubble();
					}
					incomingTextView.setText(currentText + (char)character);
				}
				if(incomingTextView.getText().toString().trim().length()==0)
					incomingTextView.setVisibility(View.GONE);
				else if(incomingTextView.getVisibility() != View.VISIBLE)
					incomingTextView.setVisibility(View.VISIBLE);
				rtt_scrollview.post(new Runnable() {
					@Override
					public void run() {
						rtt_scrollview.fullScroll(ScrollView.FOCUS_DOWN);
					}
				});

			}
		});


		//int scroll_amount = (incomingTextView.getLineCount() * incomingTextView.getLineHeight()) - (incomingTextView.getBottom() - incomingTextView.getTop());
		//incomingTextView.scrollTo(0, (int) (scroll_amount + incomingTextView.getLineHeight() * 0.5));
	}
	public TextView create_new_incoming_bubble(){
		isIncomingBubbleCreated = true;
		LinearLayout.LayoutParams lp1=new LinearLayout.LayoutParams(to_dp(300), LinearLayout.LayoutParams.WRAP_CONTENT);
		lp1.setMargins(0, 0, to_dp(10), 0);
		lp1.gravity = Gravity.RIGHT;
		TextView tv=new TextView(this);
		tv.setLayoutParams(lp1);
		if(LinphonePreferences.instance().isForce508()){
			tv.setBackgroundResource(R.drawable.chat_bubble_incoming_508);
		}else{
			tv.setBackgroundResource(R.drawable.chat_bubble_incoming);
		}
		tv.setTag(false);

		standardize_bubble_view(tv);

		tv.setTextColor(Color.parseColor("#000000"));
		tv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				outgoingEditText.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

			}
		});
		incomingTextView=tv;
		((LinearLayout)rttContainerView).addView(tv);

		rtt_scrollview.post(new Runnable() {
			@Override
			public void run() {
				rtt_scrollview.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		rttIncomingBubbleCount++;
		return tv;
	}
	private void showRTTinterface() {
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		/*runOnUiThread(new Runnable() {
			public void run() {
				isRTTMaximized = true;
				rttHolder.setVisibility(View.VISIBLE);
			}
		});*/

		isRTTMaximized = true;
		rttHolder.setVisibility(View.VISIBLE);
	}
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mControlsLayout.setVisibility(View.VISIBLE);
	}

	/** Called when backspace is pressed in an RTT conversation.
	 * Sends a backspace character and updates the outgoing text
	 * views if necessary.
	 * @return true if the key event should sbe consumed (ie. there should
	 * be no further processing of this backspace event)
	 */
//	private boolean backspacePressed() {
//		if (rttOutputEditTexts.getText().length() == 0) {
//			rttOutputEditTexts.removeTextChangedListener(rttTextWatcher);
//
//			// If there's no text in the input EditText, check if
//			// there's any old sent text that can be brought down.
//			// Lines are delimited by \n in this simple text UI.
//
//			String outtext = rttOutputEditTexts.getText().toString();
//			int newline = outtext.lastIndexOf("\n");
//
//			if (newline >= 0) {
//				rttOutputEditTexts.setText(outtext.substring(0, newline));
//				rttOutputEditTexts.append(outtext.substring(newline+1));
//			} else {
//				rttOutputField.setText("");
//				rttOutputEditTexts.append(outtext);
//			}
//			rttOutputEditTexts.addTextChangedListener(rttTextWatcher);
//			return true;
//		} else {
//
//			sendRttCharacter((char) 8);
//
//			if (hasHardwareKeyboard()) {
//				rttOutputEditTexts.removeTextChangedListener(rttTextWatcher);
//
//				// Quick and dirty hack to keep the cursor at the end of the line.
//				// EditText.append() inserts the text and places the cursor last.
//				CharSequence cs = rttOutputEditTexts.getText();
//				rttOutputEditTexts.setText("");
//				rttOutputEditTexts.append(cs.subSequence(0, cs.length() - 1));
//
//				rttOutputField.addTextChangedListener(rttTextWatcher);
//			}
//
//			return true;
//		}
//	}

	/** Somewhat reliable method of detecting the presence of a hardware
	 * keyboard. Not fully tested, needs to work for both Bluetooth and USB.
	 * @return true if a hardware keyboard is present
	 */
	private boolean hasHardwareKeyboard() {
		Resources res = getApplicationContext().getResources();
		return res.getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY;
	}

	/** Called when the user has pressed enter in an RTT conversation. This
	 * method inserts line breaks in the text views and sends the appropriate
	 * newline character.
	 */
//	private void enterPressed() {
//		rttOutputEditTexts.removeTextChangedListener(rttTextWatcher);
//		//rttOutgoingTextView.setText(rttOutgoingTextView.getText() + "\n" + rttOutputEditTexts.getText());
//		rttOutgoingTextView.append("\n");
//		rttOutgoingTextView.append(rttOutputEditTexts.getText());
//
//		int scroll_amount = (rttOutgoingTextView.getLineCount() * rttOutgoingTextView.getLineHeight()) - (rttOutgoingTextView.getBottom() - rttOutgoingTextView.getTop());
//		rttOutgoingTextView.scrollTo(0, (int) (scroll_amount + rttOutgoingTextView.getLineHeight() * 0.5));
//
//		rttOutputEditTexts.setText("");
//		sendRttCharacter((char) 10);
//
//		//rttOutputEditTexts.addTextChangedListener(rttTextWatcher);
//	}

	/** Send a single character in RTT */
	private void sendRttCharacter(char character) {
		sendRttCharacterSequence(String.valueOf(character));
	}

	/** Send a sequence of characters in RTT */
	private void sendRttCharacterSequence(CharSequence cs) {

		if (cs.length() > 0) {
			Log.d("RTT","LinphoneManager.getInstance().sendRealtimeText(cs);"+cs);
			LinphoneManager.getInstance().sendRealtimeText(cs);
		}
	}

	private boolean isVideoEnabled(LinphoneCall call) {
		if(call != null){
			return call.getCurrentParamsCopy().getVideoEnabled();
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("isRTTMaximized", isRTTMaximized);
		outState.putBoolean("Mic", LinphoneManager.getLc().isMicMuted());
		outState.putBoolean("AudioMuted", LinphoneManager.getLc().getPlaybackGain() == mute_db);
		outState.putBoolean("VideoCallPaused", isVideoCallPaused);

		super.onSaveInstanceState(outState);
	}

	public void save_messages(){

		LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if(call == null) {
			delete_messages();
			return;
		}
		Log.d("saving RTT view");
		//Store RTT or SIP SIMPLE text log
		int number_of_messages=((LinearLayout) rttContainerView).getChildCount();
		LinphoneActivity.instance().message_directions=new int[number_of_messages + 1];
		LinphoneActivity.instance().message_texts=new String[number_of_messages + 1];
		for(int j=0; j<number_of_messages; j++){
			View view=((LinearLayout) rttContainerView).getChildAt(j);
			if(((Boolean)view.getTag())){
				LinphoneActivity.instance().message_directions[j]=OUTGOING;
			}else{
				LinphoneActivity.instance().message_directions[j]=INCOMING;
			}
			LinphoneActivity.instance().message_texts[j]=((TextView)view).getText().toString();
		}
		LinphoneActivity.instance().message_texts[number_of_messages]=(outgoingEditText).getText().toString();
		LinphoneActivity.message_call_Id = call.getCallLog().getCallId();
	}

	public void populate_messages(){
		LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if(call == null || !call.getCallLog().getCallId().equals( LinphoneActivity.message_call_Id) ){
			return;
		}
		int[] direction=LinphoneActivity.instance().message_directions;
		String[] messages=LinphoneActivity.instance().message_texts;
		Log.d("openning saved RTT view");
		for(int i=0; i<messages.length-1; i++){

			if(direction[i]==OUTGOING){
				Log.d("OUTGOING: "+messages[i]);
				create_new_outgoing_bubble(null, false).setText(messages[i]);
			}else{
				Log.d("INCOMING: "+messages[i]);
				create_new_incoming_bubble();
				incomingTextView.setText(messages[i]);
			}

		}
		outgoingEditText.setText(messages[messages.length - 1]);

	}

	public void delete_messages(){
		LinphoneActivity.instance().message_directions=null;
		LinphoneActivity.instance().message_texts=null;
	}

	private boolean isTablet() {
		return getResources().getBoolean(R.bool.isTablet);
	}

	private void initUI() {
		inflater = LayoutInflater.from(this);

		video = (ImageView) findViewById(R.id.video);

		video.setOnClickListener(this);
		video.setEnabled(false);


		micro = (ImageView) findViewById(R.id.micro);
		micro.setOnClickListener(this);
//		micro.setEnabled(false);
		audioMute = (ImageView) findViewById(R.id.audioMute);
		audioMute.setOnClickListener(this);
		//toggleSpeaker(isSpeakerMuted);

		addCall = (TextView) findViewById(R.id.addCall);
		addCall.setOnClickListener(this);
		addCall.setEnabled(false);

		toggleSpeaker = (TextView) findViewById(R.id.toggleSpeaker);
		toggleSpeaker.setOnClickListener(this);

		transfer = (TextView) findViewById(R.id.transfer);
		transfer.setOnClickListener(this);
		transfer.setEnabled(false);
		options = (ImageView) findViewById(R.id.options);
		options.setOnClickListener(this);
		options.setEnabled(false);
		chat_button = (TextView) findViewById(R.id.toggleChat);
		chat_button.setOnClickListener(this);
		chat_button.setEnabled(false);
		hangUp = (TextView) findViewById(R.id.hangUp);
		hangUp.setOnClickListener(this);

		acceptBtn = findViewById(R.id.accept_call_button);
		acceptBtn.setOnClickListener(this);


		declineBtn = findViewById(R.id.decline_call_button);
		declineBtn.setOnClickListener(this);

		callLaterBtn = findViewById(R.id.call_later_button);
		callLaterBtn.setOnClickListener(this);

		statusContainer = (RelativeLayout) findViewById(R.id.audioContainer);
		statusContainer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(tv_sub_status.getVisibility() == View.VISIBLE) {
					statusContainer.setVisibility(View.GONE);
				}
				if (LinphoneManager.getLc().getCallsNb() == 0)
						finish();


			}
		});
		//TODO: remove
//		conference = (TextView) findViewById(R.id.conference);
//		conference.setOnClickListener(this);

		dialer = (ImageView) findViewById(R.id.dialer);
		dialer.setOnClickListener(this);
		dialer.setEnabled(false);
		numpad = (Numpad) findViewById(R.id.numpad);
		numpad.setHapticEnabled(true);
		numpad.setDTMFSoundEnabled(false);

		//TODO: remove
//		videoProgress =  (ProgressBar) findViewById(R.id.videoInProgress);
//		videoProgress.setVisibility(View.GONE);



		try {
			routeLayout = (LinearLayout) findViewById(R.id.routesLayout);
			audioRoute = (ImageView) findViewById(R.id.audioRoute);
			audioRoute.setOnClickListener(this);
			routeSpeaker = (TextView) findViewById(R.id.routeSpeaker);
			routeSpeaker.setOnClickListener(this);
			routeReceiver = (TextView) findViewById(R.id.routeReceiver);
			routeReceiver.setOnClickListener(this);
			routeBluetooth = (TextView) findViewById(R.id.routeBluetooth);
			routeBluetooth.setOnClickListener(this);
		} catch (NullPointerException npe) {
			Log.e("Bluetooth: Audio routes menu disabled on tablets for now (1)");
		}



		switchCamera = (TextView) findViewById(R.id.switchCamera);
		switchCamera.setOnClickListener(this);

		mControlsLayout = (ViewGroup) findViewById(R.id.menu);

		mIncomingcallsLayout = (LinearLayout) findViewById(R.id.second_incoming_call_controllers);
		mInComingCallHeader = (RelativeLayout) findViewById(R.id.layout_item_in_call_incoming);
		mInComingCallHeader.setOnClickListener(this);
		mInPassiveCallHeader = (RelativeLayout) findViewById(R.id.layout_item_in_call_passive);
		mInPassiveCallHeader.setOnClickListener(this);

		if (!isTransferAllowed) {
			addCall.setBackgroundResource(R.drawable.options_add_call);
		}
		if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
			if(!BluetoothManager.getInstance().isBluetoothHeadsetAvailable()) {
				BluetoothManager.getInstance().initBluetooth();
			}
		}

		if (!isAnimationDisabled) {
			slideInRightToLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_to_left);
			slideOutLeftToRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_left_to_right);
			slideInBottomToTop = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom_to_top);
			slideInTopToBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_top_to_bottom);
			slideOutBottomToTop = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom_to_top);
			slideOutTopToBottom = AnimationUtils.loadAnimation(this, R.anim.slide_out_top_to_bottom);
		}

		if (BluetoothManager.getInstance().isBluetoothHeadsetAvailable()) {
			try {
				if (routeLayout != null)
					routeLayout.setVisibility(View.VISIBLE);
				audioRoute.setVisibility(View.VISIBLE);
				audioMute.setVisibility(View.GONE);
			} catch (NullPointerException npe) { Log.e("Bluetooth: Audio routes menu disabled on tablets for now (2)"); }
		} else {
			try {
				if (routeLayout != null)
					routeLayout.setVisibility(View.GONE);
				audioRoute.setVisibility(View.GONE);
				audioMute.setVisibility(View.VISIBLE);
			} catch (NullPointerException npe) { Log.e("Bluetooth: Audio routes menu disabled on tablets for now (3)"); }
		}


		LinphoneManager.getInstance().changeStatusToOnThePhone();

//		if(isCameraMutedOnStart) {
//			Log.d("isCameraMutedOnStart3");
//			setCameraMute(isCameraMuted);

//			video.setSelected(true);
//			isCameraMutedOnStart = false;
//		}

		initIncomingCallsViews();

	}

	private void refreshInCallActions() {
		LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if (!LinphonePreferences.instance().isVideoEnabled() || !isVideoEnabled(call)) {
			video.setEnabled(false);
		}
		else {
			if (!isCameraMuted) {
				video.setSelected(false);
			} else {
				video.setSelected(true);
			}
		}

		try {
			if (!isAudioMuted) {
//				speaker.setBackgroundResource(R.drawable.speaker_on);
				audioMute.setSelected(false);

				routeSpeaker.setSelected(true);
				routeReceiver.setSelected(false);
				routeBluetooth.setSelected(false);
			} else {
//				speaker.setBackgroundResource(R.drawable.selector_in_call_speaker);
				audioMute.setSelected(true);
				routeSpeaker.setSelected(false);
				if (BluetoothManager.getInstance().isUsingBluetoothAudioRoute()) {
					routeReceiver.setSelected(false);
					routeBluetooth.setSelected(true);
				} else {
					routeReceiver.setSelected(true);
					routeBluetooth.setSelected(false);
				}
			}
		} catch (NullPointerException npe) {
			Log.e("Bluetooth: Audio routes menu disabled on tablets for now (4)");
		}


		if (isMicMuted) {
			micro.setSelected(true);
		} else {
			micro.setSelected(false);
		}


		if (LinphoneManager.getLc().getCallsNb() > 1) {
			//TODO: remove
//			conference.setVisibility(View.VISIBLE);
			chat_button.setVisibility(View.GONE);
		} else {
			//TODO: remove
//			conference.setVisibility(View.GONE);

			if(chat_button.getVisibility()!=View.VISIBLE)
				chat_button.setVisibility(View.VISIBLE);

			List<LinphoneCall> pausedCalls = LinphoneUtils.getCallsInState(LinphoneManager.getLc(), Arrays.asList(State.Paused));

		}

	}

	private void enableAndRefreshInCallActions() {
		addCall.setEnabled(LinphoneManager.getLc().getCallsNb() < LinphoneManager.getLc().getMaxCalls());
		transfer.setEnabled(getResources().getBoolean(R.bool.allow_transfers));
		options.setEnabled(!getResources().getBoolean(R.bool.disable_options_in_call) && (addCall.isEnabled() || transfer.isEnabled()));

		if(LinphoneManager.getLc().getCurrentCall() != null && LinphonePreferences.instance().isVideoEnabled() && !LinphoneManager.getLc().getCurrentCall().mediaInProgress()) {
			video.setEnabled(true);
		}

		micro.setEnabled(true);
		audioMute.setEnabled(true);

		transfer.setEnabled(true);
		chat_button.setEnabled(true);
		dialer.setEnabled(true);
		//TODO: remove
//		conference.setEnabled(true);

		refreshInCallActions();
	}

	public void update_call(){//This function fixes situation when on device isn't sending video. Execute it on the device that isn't sending video.
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			//lc.setDeviceRotation(90);
			LinphoneCall currentCall = lc.getCurrentCall();
			if (currentCall != null && currentCall.cameraEnabled() && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
				lc.updateCall(currentCall, null);
			}
		}
	}
	public void updateStatusFragment(StatusFragment statusFragment) {
		status = statusFragment;
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		if(isRTTMaximized){
			hideRTTinterface();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			isRTTMaximized = false;
			mControlsLayout.setVisibility(View.VISIBLE);
		}


		if (id == R.id.video) {
			toggleCamera_mute();

		}
		else if (id == R.id.micro) {
			toggleMicro();
		}
		else if (id == R.id.audioMute) {
			toggleAudioMute();

		}
		else if (id == R.id.addCall) {

			goBackToDialer();
		}

		else if (id == R.id.toggleSpeaker) {
			toggleSpeaker();
		}
		else if (id == R.id.toggleChat) {
			if(isRTTEnabled) {
				try {
					toggle_chat();
				}catch(Throwable e){
					e.printStackTrace();
				}
			}
			else{
				Toast.makeText(InCallActivity.this, "RTT has been disabled for this call", Toast.LENGTH_SHORT).show();
			}
		}

		else if (id == R.id.hangUp) {
			hangUp();
		}
		else if (id == R.id.dialer) {
			hideOrDisplayNumpad();
		}
		//TODO: remove
//		else if (id == R.id.conference) {
//			enterConference();
//		}
		else if (id == R.id.switchCamera) {
			if (videoCallFragment != null) {
				hide_controls(0);
				videoCallFragment.switchCamera();
			}
		}
		else if (id == R.id.transfer) {
			goBackToDialerAndDisplayTransferButton();
		}
		else if (id == R.id.options) {
			hideOrDisplayCallOptions();
		}
//		else if (id == R.id.audioRoute) {
//			hideOrDisplayAudioRoutes();
//		}
//		else if (id == R.id.routeBluetooth) {
//			if (BluetoothManager.getInstance().routeAudioToBluetooth()) {
//				isAudioMuted = false;
//				routeBluetooth.setSelected(true);
//				routeReceiver.setSelected(false);
//				routeSpeaker.setSelected(false);
//			}
//			hideOrDisplayAudioRoutes();
//		}
//		else if (id == R.id.routeReceiver) {
//			LinphoneManager.getInstance().routeAudioToReceiver();
//			isAudioMuted = false;
//			routeBluetooth.setSelected(false);
//			routeReceiver.setSelected(true);
//			routeSpeaker.setSelected(false);
//			hideOrDisplayAudioRoutes();
//		}
//		else if (id == R.id.routeSpeaker) {
//			LinphoneManager.getInstance().routeAudioToSpeaker();
//			routeBluetooth.setSelected(false);
//			routeReceiver.setSelected(true);
//			routeSpeaker.setSelected(true);
//			hideOrDisplayAudioRoutes();
//		}

		else if (id == R.id.callStatus) {
			LinphoneCall call = (LinphoneCall) v.getTag();
			pauseOrResumeCall(call);
		}
		//TODO: remove
		else if (id == R.id.conferenceStatus) {
			pauseOrResumeConference();
		}
		else if(id==R.id.accept_call_button)
		{
			answer();
			hideIncomingCallControlers();
		}
		else if(id==R.id.decline_call_button)
		{
			decline();
			hideIncomingCallControlers();
		}
		else if(id==R.id.call_later_button)
		{
			//is not implemented yet
		} else if (id == R.id.layout_item_in_call_passive)
			switchCalls();
//		hide_controls(0);
	}

	public void toggle_chat() {
		Log.d("RTT", "toggleChat clicked");
		Log.d("RTT", "isRTTMaximaized" + isRTTMaximized);
		mControlsLayout.setVisibility(View.INVISIBLE);

		if(isRTTMaximized){
			hideRTTinterface();
		} else{
			Log.d("rttOutgoingBubbleCount"+rttOutgoingBubbleCount);
//			if(rttOutgoingBubbleCount==0){
//				create_new_outgoing_bubble(null, true);
//			}
			showRTTinterface();
			outgoingEditText.requestFocus();
			((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
					.showSoftInput(outgoingEditText, InputMethodManager.SHOW_FORCED);
		}

	}
	public void hideRTTinterface(){
		if(rttHolder!=null) {
			rttHolder.setVisibility(View.GONE);
			rttHolder.setVisibility(View.GONE);
			isRTTMaximized=false;
			mControlsLayout.setVisibility(View.VISIBLE);
		}
	}


	private void setCameraMute(boolean muted)	{
		if(isEmergencyCall)
			muted = false;

		isCameraMuted = muted;


		if (!muted) {
			LinphoneManager.getInstance().sendStaticImage(false);
		} else {
			LinphoneManager.getInstance().sendStaticImage(true);
		}

		//This line remains for other platforms. To force the video to unfreeze.

	}
	public boolean isCameraMuted()
	{
		return  isCameraMuted;
	}

	public void toggleCamera_mute() {
		video.setSelected(!isCameraMuted);
		setCameraMute(!isCameraMuted);

	}

	public void displayCustomToast(final String message, final int duration) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toastRoot));

		TextView toastText = (TextView) layout.findViewById(R.id.toastMessage);
		toastText.setText(message);

		final Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setDuration(duration);
		toast.setView(layout);
		toast.show();
	}

	private void switchVideo(final boolean displayVideo) {
		final LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if (call == null) {
			return;
		}

		//Check if the call is not terminated
		if(call.getState() == State.CallEnd || call.getState() == State.CallReleased) return;

		if (!displayVideo) {
			showAudioView();
		} else {
			if (!call.getRemoteParams().isLowBandwidthEnabled()) {
				LinphoneManager.getInstance().addVideo();
				if (videoCallFragment == null || !videoCallFragment.isVisible())
					showVideoView();
			} else {
				displayCustomToast(getString(R.string.error_low_bandwidth), Toast.LENGTH_LONG);
			}
		}
	}

	private void showAudioView() {
		video.setSelected(false);
		LinphoneManager.startProximitySensorForActivity(InCallActivity.this);
		startOutgoingRingCount();
		setCallControlsVisibleAndRemoveCallbacks();
	}

	private void showVideoView() {

		labelRingingView.setVisibility(View.GONE);
		outboundRingCountView.setVisibility(View.GONE);
		invalidateStatusContainer();

		if (!BluetoothManager.getInstance().isBluetoothHeadsetAvailable()) {
			Log.w("Bluetooth not available, using speaker");
			LinphoneManager.getInstance().routeAudioToSpeaker();
//				speaker.setBackgroundResource(R.drawable.speaker_on);
			audioMute.setSelected(isAudioMuted);
		}
		video.setSelected(true);
		video.setEnabled(true);

		//TODO: remove
//			videoProgress.setVisibility(View.INVISIBLE);

		LinphoneManager.stopProximitySensorForActivity(InCallActivity.this);
		//replaceFragmentAudioByVideo();
		displayVideoCallControlsIfHidden(SECONDS_BEFORE_HIDING_CONTROLS);

	}



	private void toggleMicro() {
		LinphoneCore lc = LinphoneManager.getLc();
		isMicMuted = !isMicMuted;
		if(isEmergencyCall)
			isMicMuted = false;
		lc.muteMic(isMicMuted);
		if (isMicMuted) {
			micro.setSelected(true);
		} else {
			micro.setSelected(false);
		}
	}

	private void toggleAudioMute() {
		final float mute_db = -1000.0f;
		if (isAudioMuted) {
			LinphoneManager.getLc().setPlaybackGain(0);
			isAudioMuted=false;
		} else {
			LinphoneManager.getLc().setPlaybackGain(mute_db);
			isAudioMuted=true;
		}
		audioMute.setSelected(isAudioMuted);
	}

	private void toggleSpeaker(){
		final float mute_db = -1000.0f;
		if(isAudioMuted){
			//Unmute it first
			LinphoneManager.getLc().setPlaybackGain(0);
		}

		if(isSpeakerOn){
			LinphoneManager.getInstance().routeAudioToReceiver();
			isSpeakerOn=false;
			if(!wakeLock.isHeld()) {
				wakeLock.acquire();
			}


		}else{
			LinphoneManager.getInstance().routeAudioToSpeaker();
			isSpeakerOn=true;
			if(wakeLock.isHeld()) {
				wakeLock.release();
			}
		}


	}

	private void pauseOrResumeCall() {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null && lc.getCallsNb() >= 1) {
			LinphoneCall call = lc.getCalls()[0];
			pauseOrResumeCall(call);
		}
	}

	public void pauseOrResumeCall(LinphoneCall call) {
		LinphoneCore lc = LinphoneManager.getLc();
		if (call != null && LinphoneUtils.isCallRunning(call)) {

			//TODO: remove
			if (call.isInConference()) {
				lc.removeFromConference(call);
				if (lc.getConferenceSize() <= 1) {
					lc.leaveConference();
				}
			} else {
				lc.pauseCall(call);
				if (isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
					isVideoCallPaused = true;
					showAudioView();
				}
			}
		} else if (call != null) {
			if (call.getState() == State.Paused) {
				lc.resumeCall(call);
				if (isVideoCallPaused) {
					isVideoCallPaused = false;
					showVideoView();
				}
			}
		}
	}

	private void hangUp() {
		finishWithoutDelay = true;
		LinphoneCore lc = LinphoneManager.getLc();
		int call_number  = lc.getCallsNb();
		LinphoneCall currentCall = lc.getCurrentCall();

		if (currentCall != null) {
			lc.terminateCall(currentCall);
		} else if (lc.isInConference()) {
			lc.terminateConference();
		} else {
			lc.terminateAllCalls();
		}
		delete_messages();


		if(call_number == 0)
			finish();
	}

	private void enterConference() {
		LinphoneManager.getLc().addAllToConference();
	}

	public void pauseOrResumeConference() {
		LinphoneCore lc = LinphoneManager.getLc();
		if (lc.isInConference()) {
			lc.leaveConference();
		} else {
			lc.enterConference();
		}
	}

	public void displayVideoCallControlsIfHidden(int delay_until_hide) {
		if (mControlsLayout != null) {
			if (mControlsLayout.getVisibility() != View.VISIBLE) {
				videoCallFragment.animateUpSelfView(isAnimationDisabled);
				if (isAnimationDisabled) {
					mControlsLayout.setVisibility(View.VISIBLE);

					if (cameraNumber > 1) {
						switchCamera.setVisibility(View.INVISIBLE);
					}
				} else {
					Animation animation = slideInBottomToTop;
					animation.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
							animating_show_controls = true;
							mControlsLayout.setVisibility(View.VISIBLE);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

						@Override
						public void onAnimationEnd(Animation animation) {
							animation.setAnimationListener(null);
							animating_show_controls = false;

						}
					});
					mControlsLayout.startAnimation(animation);


				}


			}
		}
	}


	public void hide_controls(int delay_until_hide) {

		options.setSelected(false);
		if(mControlsLayout.getVisibility()==View.INVISIBLE || isAnimatingHideControllers)
			return;

		if (mControlsHandler != null && mControls != null) {
			mControlsHandler.removeCallbacks(mControls);
		}
		mControls = null;

		if (isVideoEnabled(LinphoneManager.getLc().getCurrentCall()) && mControlsHandler != null) {
			if(delay_until_hide!=NEVER) {
				videoCallFragment.animateDownSelfView(isAnimationDisabled);
				mControlsHandler.postDelayed(mControls = new Runnable() {
					public void run() {
						hideNumpad();

						if (isAnimationDisabled) {
							mControlsLayout.setVisibility(View.INVISIBLE);
							numpad.setVisibility(View.GONE);
						} else {
							Animation animation = slideOutTopToBottom;
							animation.setAnimationListener(new AnimationListener() {
								@Override
								public void onAnimationStart(Animation animation) {
									video.setEnabled(false); // HACK: Used to avoid controls from being hided if video is switched while controls are hiding
									switchCamera.setVisibility(View.INVISIBLE);
									toggleSpeaker.setVisibility(View.INVISIBLE);
									isAnimatingHideControllers = true;
								}

								@Override
								public void onAnimationRepeat(Animation animation) {
								}

								@Override
								public void onAnimationEnd(Animation animation) {
									video.setEnabled(true); // HACK: Used to avoid controls from being hided if video is switched while controls are hiding
									mControlsLayout.setVisibility(View.INVISIBLE);
									numpad.setVisibility(View.GONE);

									animation.setAnimationListener(null);
									isAnimatingHideControllers = false;
								}
							});
							mControlsLayout.startAnimation(animation);
						}
					}
				}, delay_until_hide);
			}
		}
	}

	public void setCallControlsVisibleAndRemoveCallbacks() {
		if (mControlsHandler != null && mControls != null) {
			mControlsHandler.removeCallbacks(mControls);
		}
		mControls = null;

		mControlsLayout.setVisibility(View.VISIBLE);
		switchCamera.setVisibility(View.INVISIBLE);
	}

	private void hideNumpad() {
		if (numpad == null || numpad.getVisibility() != View.VISIBLE) {
			return;
		}

//		dialer.setBackgroundResource(R.drawable.dialer_alt);
		dialer.setSelected(false);
		if (isAnimationDisabled) {
			numpad.setVisibility(View.GONE);
		} else {
			Animation animation = slideOutTopToBottom;
			animation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					numpad.setVisibility(View.GONE);
					animation.setAnimationListener(null);
				}
			});
			numpad.startAnimation(animation);
		}
	}

	private void hideOrDisplayNumpad() {
		if (numpad == null) {
			return;
		}

		if (numpad.getVisibility() == View.VISIBLE) {
			hideNumpad();
		} else {
//			dialer.setBackgroundResource(R.drawable.dialer_alt_back);
			dialer.setSelected(true);

			if (isAnimationDisabled) {
				numpad.setVisibility(View.VISIBLE);
			} else {
				Animation animation = slideInBottomToTop;
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						numpad.setVisibility(View.VISIBLE);
						animation.setAnimationListener(null);
					}
				});
				numpad.startAnimation(animation);
			}
		}
	}

	private void hideAnimatedPortraitCallOptions() {
		Animation animation = slideOutLeftToRight;
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (isTransferAllowed) {
					transfer.setVisibility(View.INVISIBLE);
				}
				switchCamera.setVisibility(View.INVISIBLE);
				toggleSpeaker.setVisibility(View.INVISIBLE);
//				addCall.setVisibility(View.INVISIBLE);
				animation.setAnimationListener(null);
			}
		});

//		addCall.startAnimation(animation);
		switchCamera.startAnimation(animation);
		toggleSpeaker.startAnimation(animation);
	}

	private void hideAnimatedLandscapeCallOptions() {
		Animation animation = slideOutLeftToRight;
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (isTransferAllowed) {
					transfer.setVisibility(View.INVISIBLE);
				}
				switchCamera.setVisibility(View.INVISIBLE);
				toggleSpeaker.setVisibility(View.INVISIBLE);
//				addCall.setVisibility(View.INVISIBLE);
				animation.setAnimationListener(null);
			}
		});

//		addCall.startAnimation(animation);
		switchCamera.startAnimation(animation);
		toggleSpeaker.startAnimation(animation);
	}

	private void showAnimatedPortraitCallOptions() {
		Animation animation = slideInRightToLeft;
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {

				switchCamera.setVisibility(View.VISIBLE);
				toggleSpeaker.setVisibility(View.VISIBLE);
				options.setSelected(true);
//				addCall.setVisibility(View.VISIBLE);
				animation.setAnimationListener(null);
			}
		});
//		addCall.startAnimation(animation);
		switchCamera.startAnimation(animation);
		toggleSpeaker.startAnimation(animation);
	}

	private void showAnimatedLandscapeCallOptions() {
		Animation animation = slideInRightToLeft;
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {

				switchCamera.setVisibility(View.VISIBLE);
				toggleSpeaker.setVisibility(View.VISIBLE);
				options.setSelected(true);
//				addCall.setVisibility(View.VISIBLE);
				animation.setAnimationListener(null);
			}
		});
		switchCamera.startAnimation(animation);
		toggleSpeaker.startAnimation(animation);
	}

	private void hideOrDisplayAudioRoutes() {
		if (routeSpeaker.getVisibility() == View.VISIBLE) {
			routeSpeaker.setVisibility(View.INVISIBLE);
			routeBluetooth.setVisibility(View.INVISIBLE);
			routeReceiver.setVisibility(View.INVISIBLE);
			audioRoute.setSelected(false);
		} else {
			routeSpeaker.setVisibility(View.VISIBLE);
			routeBluetooth.setVisibility(View.VISIBLE);
			routeReceiver.setVisibility(View.VISIBLE);
			audioRoute.setSelected(true);
		}
	}

	private void hideOrDisplayCallOptions() {
		boolean isOrientationLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

		if (switchCamera.getVisibility() == View.VISIBLE) {

			options.setSelected(false);

			if (isAnimationDisabled) {
//				addCall.setVisibility(View.INVISIBLE);
				switchCamera.setVisibility(View.INVISIBLE);
				toggleSpeaker.setVisibility(View.INVISIBLE);
			} else {
				if (isOrientationLandscape) {
					hideAnimatedLandscapeCallOptions();
				} else {
					hideAnimatedPortraitCallOptions();
				}
			}
		} else  {
			update_call();//Adding secret refresh option, to fix when android tv (or any device doesn't refresh).
			options.setSelected(true);

			if (isAnimationDisabled)
			{
				switchCamera.setVisibility(View.VISIBLE);
				toggleSpeaker.setVisibility(View.VISIBLE);
			}
			else
			{
				if (isOrientationLandscape) {
					showAnimatedLandscapeCallOptions();
				} else {
					showAnimatedPortraitCallOptions();
				}
			}
//				addCall.setVisibility(View.VISIBLE);



		}
//		options.setSelected(true);//TODO???????????
		transfer.setEnabled(LinphoneManager.getLc().getCurrentCall() != null);
	}

	public void goBackToDialer()
	{
		goBackToDialer(!finishWithoutDelay);
		finishWithoutDelay = false;
	}

	public void goBackToDialer(boolean applyDelay) {
		Intent intent = new Intent();
		intent.putExtra("Transfer", false);
		setResult(Activity.RESULT_FIRST_USER, intent);
		//finish();asd
		if(applyDelay)
			finishWithDelay();
		else
			finish();
	}

	private void goBackToDialerAndDisplayTransferButton() {
		Intent intent = new Intent();
		intent.putExtra("Transfer", true);
		setResult(Activity.RESULT_FIRST_USER, intent);
		finish();
	}

	public void acceptCallUpdate(boolean accept) {
		if (timer != null) {
			timer.cancel();
		}

		LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if (call == null) {
			return;
		}

		LinphoneCallParams params = call.getCurrentParamsCopy();
		if (accept) {
			params.setVideoEnabled(true);
			LinphoneManager.getLc().enableVideo(true, true);
		}

		try {
			LinphoneManager.getLc().acceptCallUpdate(call, params);
		} catch (LinphoneCoreException e) {
			e.printStackTrace();
		}
	}

	public void startIncomingCallActivity() {
		//startActivity(new Intent(this, IncomingCallActivity.class));
		LinphoneManager.startIncomingCallActivity(this);
	}





	private void showAcceptCallUpdateDialog() {
		FragmentManager fm = getSupportFragmentManager();
		callUpdateDialog = new AcceptCallUpdateDialogFragment();
		callUpdateDialog.show(fm, "Accept Call Update Dialog");
	}

	@Override
	protected void onResume() {
//		try {
//			populate_messages();
//		}catch(Throwable e){
//			//No messages to populate
//			e.printStackTrace();
//		}
		Log.d("TAG", "onResume() start");
		instance = this;

		if (isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {

			displayVideoCallControlsIfHidden(SECONDS_BEFORE_HIDING_CONTROLS);
		} else {
			LinphoneManager.startProximitySensorForActivity(this);
			setCallControlsVisibleAndRemoveCallbacks();
		}
		super.onResume();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.addListener(mListener);
		}

		refreshCallList(getResources());

		handleViewIntent();

		IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(myReceiver, filter);

		try {
			LinphoneManager.getLc().muteMic(isMicMuted);
		}catch(Throwable e){
			e.printStackTrace();
		}
		update_call();
	}

	private void handleViewIntent() {
		Intent intent = getIntent();
		if(intent != null && intent.getAction() == "android.intent.action.VIEW") {
			LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
			if(call != null && isVideoEnabled(call)) {
				LinphonePlayer player = call.getPlayer();
				String path = intent.getData().getPath();
				Log.i("Openning " + path);
				int openRes = player.open(path, new LinphonePlayer.Listener() {

					@Override
					public void endOfFile(LinphonePlayer player) {
						player.close();
					}
				});
				if(openRes == -1) {
					String message = "Could not open " + path;
					Log.e(message);
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
					return;
				}
				Log.i("Start playing");
				if(player.start() == -1) {
					player.close();
					String message = "Could not start playing " + path;
					Log.e(message);
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	protected void onPause() {
		unregisterReceiver(myReceiver);
		save_messages();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.removeListener(mListener);
		}

		super.onPause();
/// onpause is called when new call is arrived
//		if (mControlsHandler != null && mControls != null) {
//			mControlsHandler.removeCallbacks(mControls);
//		}
//		mControls = null;


		if (!isVideoEnabled(LinphoneManager.getLc().getCurrentCall())) {
			LinphoneManager.stopProximitySensorForActivity(this);
		}
		Log.d("onPause finished");


		LinphoneCall call = LinphoneManager.getLc().getCurrentCall();
		if(call != null){
			g.in_call_activity_suspended=true;
			g.isRTTMaximized=isRTTMaximized;
			g.Mic= LinphoneManager.getLc().isMicMuted();
			g.AudioMuted=LinphoneManager.getLc().getPlaybackGain() == mute_db;
			g.VideoCallPaused=isVideoCallPaused;
		}else{
			//call terminated either remotely or locally
			g.in_call_activity_suspended=false;
		}
	}

	@Override
	protected void onDestroy() {
		if(mHandler!= null )
		{
			mHandler.removeCallbacks(finishRunnable);
			mHandler.removeMessages(1);
			mHandler = null;
		}

		if(isFlashing) {
			isFlashing = false;
			LinphoneTorchFlasher.instance().stopFlashTorch();
			HueController.getInstance().stopFlashing();
		}
		LinphoneService.instance().setActivityToLaunchOnIncomingReceived(LinphoneActivity.class);
		LinphoneManager.getInstance().changeStatusToOnline();

		if (mControlsHandler != null && mControls != null) {
			mControlsHandler.removeCallbacks(mControls);
		}
		mControls = null;
		mControlsHandler = null;

		unbindDrawables(findViewById(R.id.topLayout));
		instance = null;
		super.onDestroy();
		System.gc();

		if(wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	private void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ImageView) {
			view.setOnClickListener(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (LinphoneUtils.onKeyVolumeAdjust(keyCode)) return true;
		if (LinphoneUtils.onKeyBackGoHome(this, keyCode, event)) return true;
		return super.onKeyDown(keyCode, event);
	}

	public void bindVideoFragment(VideoCallFragment fragment) {
		videoCallFragment = fragment;
	}


	private void setContactName(TextView contact, LinphoneAddress lAddress, String sipUri, Resources resources) {
		//TextView partnerName = (TextView) findViewById(R.id.partner_name);
		//TextView userName = (TextView) findViewById(R.id.user_name);
		LinphonePreferences mPrefs = LinphonePreferences.instance();
		String username = mPrefs.getAccountUsername(mPrefs.getDefaultAccountIndex());
		//userName.setText(username);

		Contact lContact  = ContactsManager.getInstance().findContactWithAddress(contact.getContext().getContentResolver(), lAddress);
		if (lContact == null) {
			if (resources.getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(sipUri)) {
				contact.setText(lAddress.getUserName());
				contactName = lAddress.getUserName();
				android.util.Log.e("Info", "contactName = " + contactName);
				//partnerName.setText(contactName);
			} else {
				contact.setText(sipUri);
				contactName = sipUri;
				android.util.Log.e("Info", "contactName = " + contactName);
				// partnerName.setText(contactName);
			}
		} else {
			contact.setText(lContact.getName());
			contactName = lContact.getName();
			android.util.Log.e("Info", "contactName = " + contactName);
			//partnerName.setText(contactName);
		}
	}

	public void startOutgoingRingCount() {
//		statusContainer.setVisibility(View.VISIBLE);
		labelRingingView.setVisibility(View.VISIBLE);
		outboundRingCountView.setVisibility(View.VISIBLE);
		invalidateStatusContainer();
		outgoingRingCountTimer = new Timer();
		float outGoingRingDuration = LinphonePreferences.instance().getConfig().getFloat("vtcsecure", "outgoing_ring_duration", 2.0f);

		outgoingRingCountTimer.schedule(new TimerTask() {
			int ringCount = 0;

			@Override
			public void run() {
				InCallActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ringCount++;
						outboundRingCountView.setText(ringCount + "");
					}
				});
			}
		}, 0, (long) (outGoingRingDuration * 1000));
	}

	private void stopOutgoingRingCount() {



		if (outgoingRingCountTimer != null) {
			outgoingRingCountTimer.cancel();
			outgoingRingCountTimer = null;
			//findViewById(R.id.outboundRingCount).setVisibility(View.GONE);
			//findViewById(R.id.label_ringing).setVisibility(View.INVISIBLE);
		}
	}


	private void displayOrHideContactPicture(ImageView callView, Uri pictureUri, Uri thumbnailUri, boolean hide) {
		if(pictureUri == null && pictureUri == null)
		{
			callView.setImageResource(R.drawable.unknown_small);
			return;
		}
		String rawContactId = null;
		try{
			rawContactId = ContactsManager.getInstance().findRawContactID(LinphoneActivity.instance().getContentResolver(), String.valueOf(contact.getID()));
		}catch(Throwable e){

			e.printStackTrace();
		}


		if (pictureUri != null) {
			LinphoneUtils.setImagePictureFromUri(callView.getContext(), callView, Uri.parse(pictureUri.toString()), thumbnailUri, R.drawable.unknown_small);
		}else if(rawContactId!=null&&ContactsManager.picture_exists_in_storage_for_contact(rawContactId)){
			callView.setImageBitmap(ContactsManager.get_bitmap_by_contact_resource_id(rawContactId));
		}

		callView.setVisibility(hide ? View.GONE : View.VISIBLE);
	}

	private void setRowBackground(LinearLayout callView, int index) {
		int backgroundResource;
		if (index == 0) {
//			backgroundResource = active ? R.drawable.cell_call_first_highlight : R.drawable.cell_call_first;
			backgroundResource = R.drawable.cell_call_first;
		} else {
//			backgroundResource = active ? R.drawable.cell_call_highlight : R.drawable.cell_call;
			backgroundResource = R.drawable.cell_call;
		}
		callView.setBackgroundResource(backgroundResource);
	}

	private void registerCallDurationTimer(View v, LinphoneCall call) {
		int callDuration = call.getDuration();
		if (callDuration == 0 && call.getState() != State.StreamsRunning) {
			return;
		}

		Chronometer timer = (Chronometer) v.findViewById(R.id.callTimer);
		if (timer == null) {
			throw new IllegalArgumentException("no callee_duration view found");
		}

		timer.setBase(SystemClock.elapsedRealtime() - 1000 * callDuration);
		timer.start();
	}

	public void refreshCallList(Resources resources) {

		if(LinphoneManager.getLc().getCalls().length==1 && LinphoneManager.getLc().getCalls()[0].getState()==State.IncomingReceived )
		{

			LinphoneManager.startIncomingCallActivity(this);
			finish();
			return;
		}

		if (LinphoneManager.getLc().getCallsNb() == 0) {
			goBackToDialer();
			return;
		}
//
//		if(LinphoneManager.getLc().getCurrentCall() == null){
//			showAudioView();
//			video.setEnabled(false);
//		}
//
//		callsList.invalidate();
	}
	private class HeadPhoneJackIntentReceiver extends BroadcastReceiver {
		@Override public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra("state", -1);
				switch (state) {
					case 0:
						Log.d("HEADPHONES", "Headset is unplugged");

						break;
					case 1:
						Log.d("HEADPHONES", "Headset is plugged");
						LinphoneManager.getInstance().routeAudioToReceiver();
						break;
					default:
						Log.d("HEADPHONES", "I have no idea what the headset state is");
				}
			}
		}
	}


	void checkIncomingCall() {

		LinphoneCall call = getIncomingCall();
		if (call != null) {
			String sipUri = call.getRemoteAddress().asStringUriOnly();
			LinphoneAddress lAddress;
			showIncomingCallControlers();
		}
		else{
			hideIncomingCallControlers();
		}

	}
	void hideIncomingCallControlers()
	{

		displayVideoCallControlsIfHidden(SECONDS_BEFORE_HIDING_CONTROLS);
		invalidateHeader();
	}
	void showIncomingCallControlers()
	{
		// show header and start flashing
		//mIncomingcallsLayout.setVisibility(View.VISIBLE);
		hide_controls(0);
		invalidateHeader();
	}
	private LinphoneCall getIncomingCall()
	{
		LinphoneCall[] calls = LinphoneManager.getLc().getCalls();
		for (LinphoneCall call: calls
				) {
			if (call.getState() == State.IncomingReceived) {
				return call;
			}
		}
		return null;
	}

	private LinphoneCall getPassiveCall()
	{
		LinphoneCall[] calls = LinphoneManager.getLc().getCalls();
		for (LinphoneCall call: calls
				) {
			if (call!= LinphoneManager.getLc().getCurrentCall()) {
				return call;
			}
		}
		return null;
	}



	private void answer() {
		LinphoneCall mCall = getIncomingCall();
		if(mCall==null)
			return;
		LinphoneCallParams params = LinphoneManager.getLc().createCallParams(mCall);

		boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(this);
		if (isLowBandwidthConnection) {
			params.enableLowBandwidth(true);
			Log.d("Low bandwidth enabled in call params");
		}

		LinphoneCallParams callerParams = mCall.getRemoteParams();
		LinphoneManager.getInstance().setDefaultRttPreference();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());
		Log.d("RTT: pref_text_enable_key in prefs? " + prefs.contains(getResources().getString(R.string.pref_text_enable_key)));
		boolean textEnabled = prefs.getBoolean(getResources().getString(R.string.pref_text_enable_key), false);
		Log.d("RTT: textEnabled: " + textEnabled);
		if (callerParams.realTimeTextEnabled() && textEnabled) {
			Log.d("RTT: enabling RTT!");
			params.enableRealTimeText(true);
		} else {
			Log.d("RTT: disabling RTT!");
			params.enableRealTimeText(false);
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
//				LinphoneActivity.instance().startVideoActivity(mCall);
			} else {
//				LinphoneActivity.instance().startIncallActivity(mCall);
			}
		}
	}


	private void decline() {
		LinphoneCall mCall = getIncomingCall();
		if(mCall==null)
			return;
		LinphoneManager.getLc().terminateCall(mCall);

	}

	private void switchCalls() {

		LinphoneCore lc = LinphoneManager.getLc();
		LinphoneCall oldCall = (LinphoneCall)mInPassiveCallHeader.getTag();
		LinphoneCall currentCall = lc.getCurrentCall();


		if (oldCall != null && oldCall.getState() == State.Paused) {

			lc.resumeCall(oldCall);
		}
		if(currentCall!=null && LinphoneUtils.isCallRunning(currentCall))
			lc.pauseCall(currentCall);
		invalidateHeader();
	}

	void invalidateHeader() {
		System.out.println("++++++++++++++++++++++invalidateHeader" + isFlashing);
		if(LinphoneManager.getLc().getCallsNb() > 1 ) {

			LinphoneCall call = getPassiveCall();

			String sipUri = call.getRemoteAddress().asStringUriOnly();

			LinphoneAddress lAddress;

			try {
				lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
			} catch (LinphoneCoreException e) {
				Log.e("Incall activity cannot parse remote address", e);
				lAddress = LinphoneCoreFactory.instance().createLinphoneAddress("uknown", "unknown", "unkonown");

			}

			boolean isVideoCall = call.getRemoteParams().getVideoEnabled();

			//set header data
			if (call.getState() == State.IncomingReceived) {

				if (!isFlashing) {

					isFlashing = true;
					HueController.getInstance().startFlashing(null);
					vibrate();
					flashOrangeBackground();
					flashTorch();
				}
				mInComingCallHeader.setVisibility(View.VISIBLE);
				mInPassiveCallHeader.setVisibility(View.INVISIBLE);

				setContactName(mIncomingUserName, lAddress, call.getRemoteAddress().asStringUriOnly(), getResources());
				mIncomingCallCount.setText(String.valueOf(mRingCount));

				contact = ContactsManager.getInstance().findContactWithAddress(getContentResolver(), lAddress);
				if (contact != null) {
					displayOrHideContactPicture(mIncomingImage, contact.getPhotoUri(), contact.getThumbnailUri(), false);
				} else {
					displayOrHideContactPicture(mIncomingImage, null, null, false);
				}

				if(mIncomingcallsLayout.getVisibility()!= View.VISIBLE) {
					if(isAnimationDisabled)
						mIncomingcallsLayout.setVisibility(View.VISIBLE);
					else
					{
						Animation animation = slideInBottomToTop;
						animation.setAnimationListener(new AnimationListener() {
							@Override
							public void onAnimationStart(Animation animation) {
								mIncomingcallsLayout.setVisibility(View.VISIBLE);
							}

							@Override
							public void onAnimationEnd(Animation animation) {

							}

							@Override
							public void onAnimationRepeat(Animation animation) {

							}
						});
						mIncomingcallsLayout.startAnimation(animation);

					}
				}


				if (isVideoCall) {
					mIncomingCallType.setText("Is video calling you...");
				} else {
					mIncomingCallType.setText("Is audio calling you...");
				}
				//start flashing
			} else {
				if (isFlashing) {
					System.out.println("++++++++++++++++++++++" + isFlashing);
					isFlashing = false;
					LinphoneTorchFlasher.instance().stopFlashTorch();
					HueController.getInstance().stopFlashing();

				}

				stopRingCount();
				mInPassiveCallHeader.setTag(call);


				contact = ContactsManager.getInstance().findContactWithAddress(getContentResolver(), lAddress);
				if (contact != null) {
					displayOrHideContactPicture(mPassiveImage, contact.getPhotoUri(), contact.getThumbnailUri(), false);
				} else {
					displayOrHideContactPicture(mPassiveImage, null, null, false);
				}

				mInPassiveCallHeader.setVisibility(View.VISIBLE);

				if(mInComingCallHeader.getVisibility()!= View.INVISIBLE) {
					mIncomingcallsLayout.setVisibility(View.INVISIBLE);
					mInComingCallHeader.setVisibility(View.INVISIBLE);
				}

				setContactName(mIncomingPassiveUserName, lAddress, call.getRemoteAddress().asStringUriOnly(), getResources());

				mIncomingPassiveCallHoldTime.setBase(SystemClock.elapsedRealtime() - 1000 * call.getDuration());
				mIncomingPassiveCallHoldTime.start();
				//TODO: set data
			}
		} else {
			stopRingCount();
			if (isFlashing) {
				System.out.println("++++++++++++++++++++++" + isFlashing);
				isFlashing = false;
				LinphoneTorchFlasher.instance().stopFlashTorch();
				HueController.getInstance().stopFlashing();
			}
			mInComingCallHeader.setVisibility(View.INVISIBLE);
			mInPassiveCallHeader.setVisibility(View.INVISIBLE);
			mIncomingcallsLayout.setVisibility(View.INVISIBLE);
		}
	}

	private void flashOrangeBackground() {
		final Timer flashOrangeBackgroundTimer = new Timer();
		final float flashFrequencyInSeconds = LinphonePreferences.instance().getConfig().getFloat("vtcsecure", "incoming_flashred_frequency", 0.3f);
		flashOrangeBackgroundTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				InCallActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Integer colorFrom = getResources().getColor(R.color.incoming_header_drak_bg_transparent);
						Integer colorTo = getResources().getColor(R.color.incoming_light_backgtound);

						AnimatorSet animatorSet = new AnimatorSet();
						ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
						colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(ValueAnimator animator) {
								mInComingCallHeader.setBackgroundColor((Integer) animator.getAnimatedValue());
							}
						});

						ValueAnimator reverseColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorTo, colorFrom);
						reverseColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(ValueAnimator animator) {
								mInComingCallHeader.setBackgroundColor((Integer) animator.getAnimatedValue());
							}
						});
						colorAnimation.setDuration((long) (flashFrequencyInSeconds * 1000));
						reverseColorAnimation.setDuration((long) (flashFrequencyInSeconds * 1000));

						if (isFlashing) {
							//Call is showing terminated on some devices even though it is not, this is preventing red screen flashing. So I'm adding and attempt flash anyway, if it can't flash then we'll execute the anticpated terminate code.
							try {
								animatorSet.play(colorAnimation).after(reverseColorAnimation);
								animatorSet.start();
							} catch (Throwable e) {
								flashOrangeBackgroundTimer.cancel();
								e.printStackTrace();
								Log.d("incoming call is supposedly terminated and we tried to flash anyway, was unable to, so.. cancelling the flash timer");
							}

						} else {
//							animatorSet.play(colorAnimation).after(reverseColorAnimation);
//							animatorSet.start();
							flashOrangeBackgroundTimer.cancel();
						}
					}
				});
			}
		}, 0, (long) (flashFrequencyInSeconds * 2000));
	}

	private void flashTorch() {
		if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return;
		LinphoneTorchFlasher.instance().startFlashTorch();
	}


	private void vibrate() {
		final Timer vibrateTimer = new Timer();
		float vibrateFrequencyInSeconds = LinphonePreferences.instance().getConfig().getFloat("vtcsecure", "incoming_vibrate_frequency", 0.3f);
		final Vibrator v = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

		vibrateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				InCallActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!isFlashing) {
							vibrateTimer.cancel();
						} else {
							incrementRingCount();
							v.vibrate(500);
						}
					}
				});
			}
		}, 0, (long) (vibrateFrequencyInSeconds * 1000));

	}

	private void incrementRingCount() {
		mIncomingCallCount.setVisibility(View.VISIBLE);
		mRingCount++;
		mIncomingCallCount.setText(mRingCount + "");
	}

	private void stopRingCount() {
		mRingCount = 0;
		mIncomingCallCount.setVisibility(View.GONE);
		mIncomingCallCount.setText(mRingCount + "");
	}

	Runnable finishRunnable = new Runnable() {
		@Override
		public void run() {
			if (InCallActivity.isInstanciated()) {
					mHandler.removeMessages(1);
				finish();
			}
		}
	};

	void finishWithDelay() {
		if(finishWithoutDelay) {
			finish();
			return;
		}
		if(System.currentTimeMillis() - LinphoneActivity.instance().call_error_time < 5000)
		{
			tv_sub_status.setText(LinphoneActivity.instance().call_error_reason);
			tv_sub_status.setVisibility(View.VISIBLE);

			try {
				labelRingingView.setVisibility(View.GONE);
				outboundRingCountView.setVisibility(View.GONE);
			} catch (Exception ex){

			}
			invalidateStatusContainer();

		}
		if(!mHandler.hasMessages(1)) {
			mHandler.sendEmptyMessage(1);
			mHandler.postDelayed(finishRunnable, 5000);
		}
	}

	private void initShowErrorView(){
		tv_sub_status = (TextView) findViewById(R.id.tv_call_sub_status);
		outboundRingCountView  = (TextView) findViewById(R.id.outboundRingCount);
		labelRingingView = (TextView) findViewById(R.id.call_status_incall);
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleNotificationMessage();

		// we should not open message screen while in call
	}
}
