package org.linphone;

/*
 LinphoneActivity.java
 Copyright (C) 2012  Belledonne Communications, Grenoble, France

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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.Fragment.SavedState;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.StringListener;
import net.hockeyapp.android.UpdateManager;

import org.json.JSONObject;
import org.linphone.LinphoneManager.AddressType;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCallLog.CallStatus;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.LpConfig;
import org.linphone.core.PayloadType;
import org.linphone.core.Reason;
import org.linphone.mediastream.Log;
import org.linphone.setup.ApplicationPermissionManager;
import org.linphone.setup.JsonConfig;
import org.linphone.setup.RemoteProvisioningLoginActivity;
import org.linphone.setup.SetupActivity;
import org.linphone.ui.AddressText;
import org.linphone.vtcsecure.GAUtils;
import org.linphone.vtcsecure.LinphoneLocationManager;
import org.linphone.vtcsecure.Utils;
import org.linphone.vtcsecure.g;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static android.content.Intent.ACTION_MAIN;
import static org.linphone.LinphoneManager.getLc;

/**
 * @author Sylvain Berfini
 */
public class LinphoneActivity extends FragmentActivity implements OnClickListener, ContactPicked {

	private static final String UNREAD_MESSAGES = "isMessagesViewed";
	public static final String UNREAD_VIDEO_MAIL_MESSAGES = "isVideoMailMessagesViewed";
	public static ProgressDialog generic_ace_loading_dialog;

	public static final String PREF_FIRST_LAUNCH = "pref_first_launch";
	public static Context ctx;
	public static Activity act;
	public static final int WIFI_ACTIVITY_RESULT=10;
	private static final int SETTINGS_ACTIVITY = 123;
	public static final int FIRST_LOGIN_ACTIVITY = 101;
	private static final int REMOTE_PROVISIONING_LOGIN_ACTIVITY = 102;
	private static final int CALL_ACTIVITY = 19;
	private static final int CHAT_ACTIVITY = 21;

	private static boolean first_launch_boolean=true;

	private static LinphoneActivity instance;

	private StatusFragment statusFragment;
	private TextView missedCalls, missedChats, missedVideoMails;
	private LinearLayout menu, mark;
	public static RelativeLayout contacts, history, more, dialer, chat, aboutChat;
	private FragmentsAvailable currentFragment;
	private FragmentsAvailable nextFragment;
	private List<FragmentsAvailable> fragmentsHistory;
	private Fragment dialerFragment, messageListFragment, friendStatusListenerFragment;
	private ChatFragment chatFragment;
	private SavedState dialerSavedState;
	private boolean newProxyConfig;
	private boolean isAnimationDisabled = false, preferLinphoneContacts = false;
	public OrientationEventListener mOrientationHelper;
	private LinphoneCoreListenerBase mListener;

	private Animation slideOutLeftToRight, slideInRightToLeft;
	private LinearLayout mAnimateLayout;
	private TextView selfPreviewTextView;

	String previewIsEnabledKey;
	boolean isPreviewEnabled;

	String selfVideoIsEnabledKey;
	boolean isSelfViewEnabled;

	private int selectedTab;
	private boolean isMoreButtonClicked;
	private int moreMenuVisibility = View.GONE;
	private boolean isMessagesViewed;
	private TextView videoMailTextView;

	public static boolean providerLookupOperation_executed=false;

	public static View topLayout;
	private AsyncProviderLookupOperation providerLookupOperation;
	private SharedPreferences mPrefs;

	//In Call Message State Variables
	public static int[] message_directions;
	public static String[] message_texts;
	public static String message_call_Id;
	String call_error_reason;
	long call_error_time;
	private Fragment newFragment;

	static final boolean isInstanciated() {
		return instance != null;
	}
	public LinphoneActivity()
	{
		instance = this;
	}

	public static final LinphoneActivity instance() {
		if (instance != null)
			return instance;
		throw new RuntimeException("LinphoneActivity not instantiated yet");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("fragment", currentFragment.toString());
		outState.putInt("selected.tab", selectedTab);
		outState.putInt("more_menu_visibility", moreMenuVisibility);
		outState.putBoolean("isMoreButtonClicked", isMoreButtonClicked);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String ccc = savedInstanceState.getString("fragment");
		selectedTab = savedInstanceState.getInt("selected.tab");
		moreMenuVisibility = savedInstanceState.getInt("more_menu_visibility");
		isMoreButtonClicked = savedInstanceState.getBoolean("isMoreButtonClicked");

		if (isMoreButtonClicked) {

			if (moreMenuVisibility == View.VISIBLE)
				moreMenuVisibility = View.GONE;
			else moreMenuVisibility = View.VISIBLE;

			showHideMoreOptions();

		} else {
			moreMenuVisibility = View.GONE;
			getSelectedTab();
		}

		if (ccc != null) {
			if (ccc.equals(FragmentsAvailable.CHAT.toString()) || ccc.equals(FragmentsAvailable.CHATLIST.toString())) {
				currentFragment = FragmentsAvailable.CHATLIST;
				newFragment = new ChatListFragment();
			} else if (ccc.equals(FragmentsAvailable.CONTACT.toString()) ||ccc.equals(FragmentsAvailable.CONTACTS.toString())) {
				currentFragment = FragmentsAvailable.CONTACT;
				newFragment = new ContactsFragment();
			} else if (ccc.equals(FragmentsAvailable.HISTORY_DETAIL.toString()) || ccc.equals(FragmentsAvailable.HISTORY.toString())) {
				currentFragment = FragmentsAvailable.HISTORY;
				if (getResources().getBoolean(R.bool.use_simple_history)) {
					newFragment = new HistorySimpleFragment();
				} else {
					newFragment = new HistoryFragment();
				}
			}
		}

		if (currentFragment != null && newFragment != null) {
			changeFragmentForTablets(newFragment, currentFragment, false);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		LoginManager.register(this, "d6280d4d277d6876c709f4143964f0dc", "3e41eeed8656b90048f348c4d665a0a6", LoginManager.LOGIN_MODE_EMAIL_PASSWORD, LinphoneLauncherActivity.class);
//		LoginManager.verifyLogin(this, getIntent());
		//SetupController setupController = SetupController.getInstance();
		//if (!setupController.getSetupCompleted()){
		//navigate to the Welcome (initial set up) page
		//	startActivity(new Intent(this,HueBridgeSearchActivity.class));
		//}
		ctx=this;
		act=this;
		instance = this;
		checkForUpdates();

		if (!LinphoneLocationManager.instance(this).isLocationProviderEnabled() && !getPreferences(Context.MODE_PRIVATE).getBoolean("location_for_911_disabled_message_do_not_show_again_key", false)) {
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.location_for_911_disabled_title))
					.setMessage(getString(R.string.location_for_911_disabled_message))
					.setPositiveButton(R.string.button_ok, null)
					.setNegativeButton(R.string.location_for_911_disabled_message_do_not_show_again, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							getPreferences(Context.MODE_PRIVATE).edit().putBoolean("location_for_911_disabled_message_do_not_show_again_key", true).commit();
						}
					})
					.show();
		}

		if (isTablet() && getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
			//	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		} else if (!isTablet() && getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		if (!LinphoneManager.isInstanciated()) {
			Log.e("No service running: avoid crash by starting the launcher", this.getClass().getName());
			// super.onCreate called earlier
			finish();
			startActivity(getIntent().setClass(this, LinphoneLauncherActivity.class));
			return;
		}

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();

		if (LinphonePreferences.instance().isProvisioningLoginViewEnabled()) {
			Intent wizard = new Intent();
			wizard.setClass(this, RemoteProvisioningLoginActivity.class);
			wizard.putExtra("Domain", LinphoneManager.getInstance().wizardLoginViewDomain);
			startActivityForResult(wizard, REMOTE_PROVISIONING_LOGIN_ACTIVITY);
		} else if (LinphonePreferences.instance().isFirstLaunch() || LinphonePreferences.instance().getAccountCount() == 0 && savedInstanceState == null) {

			//This is where the login screen is launched of first run, after accept legal release
			//if(first_launch_boolean==true) {
			startActivityForResult(new Intent().setClass(this, SetupActivity.class), FIRST_LOGIN_ACTIVITY);
			LinphonePreferences.instance().firstLaunchSuccessful();
			first_launch_boolean=false;
			//}

		}

		if (getResources().getBoolean(R.bool.use_linphone_tag)) {
			ContactsManager.getInstance().initializeSyncAccount(getApplicationContext(), getContentResolver());
		} else {
			ContactsManager.getInstance().initializeContactManager(getApplicationContext(), getContentResolver());
		}

		if(!LinphonePreferences.instance().isContactsMigrationDone()){
			ContactsManager.getInstance().migrateContacts();
			LinphonePreferences.instance().contactsMigrationDone();
		}

		setContentView(R.layout.main);

		topLayout=findViewById(R.id.topLayout);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		fragmentsHistory = new ArrayList<FragmentsAvailable>();

		initButtons(LinphonePreferences.instance().isForce508());

		currentFragment = nextFragment = FragmentsAvailable.DIALER;
		fragmentsHistory.add(currentFragment);
		if (savedInstanceState == null) {
			if (findViewById(R.id.fragmentContainer) != null) {
				dialerFragment = new DialerFragment();
				dialerFragment.setArguments(getIntent().getExtras());
				getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, dialerFragment, currentFragment.toString()).commit();
				selectMenu(FragmentsAvailable.DIALER);
			}
		}

		mListener = new LinphoneCoreListenerBase(){
			@Override
			public void notifyReceived(LinphoneCore lc, LinphoneEvent ev, String eventName, LinphoneContent content) {
				super.notifyReceived(lc, ev, eventName, content);
				if (content.getSubtype().equals("simple-message-summary")) {
					try {//ng: yes
						SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
						String count_key = "Voicemail: ";
						String data = new String(content.getData());

						String count_string = data.substring(data.indexOf(count_key) + count_key.length());
						int number = Integer.parseInt(count_string.substring(0, count_string.indexOf("/")));

						prefs.edit().putInt("mwi_count", number).commit();

						String budgeKey = "Messages-Waiting: ";
						String budgedata = data.substring(data.indexOf(budgeKey) + budgeKey.length());
						budgedata = budgedata.substring(0, budgedata.indexOf("\r\n"));

						mPrefs.edit().putBoolean(UNREAD_VIDEO_MAIL_MESSAGES, !budgedata.contains("yes")).commit();

						if (missedVideoMails != null) {
							reloadMwiCount();
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
				updateMissedChatCount(true);
				if (messageListFragment != null && messageListFragment.isVisible()) {
					((ChatListFragment) messageListFragment).refresh();
				}
			}

			@Override
			public void registrationState(LinphoneCore lc, LinphoneProxyConfig proxy, LinphoneCore.RegistrationState state, String smessage) {
				if (state.equals(RegistrationState.RegistrationCleared)) {
					if (lc != null) {
						LinphoneAuthInfo authInfo = lc.findAuthInfo(proxy.getIdentity(), proxy.getRealm(), proxy.getDomain());
						if (authInfo != null)
							lc.removeAuthInfo(authInfo);
					}
				}

				if(state.equals(RegistrationState.RegistrationFailed) && newProxyConfig) {
					newProxyConfig = false;


					if (proxy.getError() == Reason.BadCredentials) {
						displayCustomToast(getString(R.string.error_bad_credentials), Toast.LENGTH_LONG);
						//go_back_to_login();
					}
					if (proxy.getError() == Reason.Unauthorized) {
						displayCustomToast(getString(R.string.error_unauthorized), Toast.LENGTH_LONG);
						//go_back_to_login();
					}
					if (proxy.getError() == Reason.IOError) {
						displayCustomToast(getString(R.string.error_io_error), Toast.LENGTH_LONG);
						//Potential Issues that through this flag
						//bad port
						//no internet connection
						//bad server address
						//mismatch transport proto-call and port number


						//throw them back to log-in


					}
				}
			}

			@Override
			public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
				//InCallActivity will handle it
				if (InCallActivity.isInstanciated())
					return;

				if (state == State.IncomingReceived) {
					LinphoneManager.startIncomingCallActivity(LinphoneActivity.this);
					//startActivity(new Intent(LinphoneActivity.instance(), IncomingCallActivity.class));
				} else if (state == State.OutgoingInit) {

					if (call.getCurrentParamsCopy().getVideoEnabled()) {
						startVideoActivity(call);
					} else {
						startIncallActivity(call);
					}
				} else if (state == State.CallEnd || state == State.Error || state == State.CallReleased) {
					if(state == State.Error){
						if(call.getErrorInfo() != null){
							//workaround for the case when call end state recieved before opening InCallActivity
							call_error_reason = Utils.getReasonText(call.getErrorInfo().getReason(), LinphoneActivity.this);
						}
						call_error_time = System.currentTimeMillis();

					}

					// Convert LinphoneCore message for internalization
					if (call.getErrorInfo() != null) {
						if (message != null && call.getErrorInfo().getReason() == Reason.Declined) {
							displayCustomToast(getString(R.string.error_call_declined), Toast.LENGTH_LONG);
						} else if (message != null && call.getErrorInfo().getReason() == Reason.NotFound) {
							displayCustomToast(getString(R.string.error_user_not_found), Toast.LENGTH_LONG);
						} else if (message != null && call.getErrorInfo().getReason() == Reason.Media) {
							displayCustomToast(getString(R.string.error_incompatible_media), Toast.LENGTH_LONG);
						} else if (message != null && state == State.Error) {
							displayCustomToast(getString(R.string.error_unknown) + " - " + message, Toast.LENGTH_LONG);
						}
					}
					resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
				}

				int missedCalls = getLc().getMissedCallsCount();
				displayMissedCalls(missedCalls);
			}
		};

		if (lc != null) {
			lc.addListener(mListener);
		}

		int missedCalls = getLc().getMissedCallsCount();
		displayMissedCalls(missedCalls);

		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		switch (rotation) {
			case Surface.ROTATION_0:
				rotation = 0;
				break;
			case Surface.ROTATION_90:
				rotation = 90;
				break;
			case Surface.ROTATION_180:
				rotation = 180;
				break;
			case Surface.ROTATION_270:
				rotation = 270;
				break;
		}

		getLc().setDeviceRotation(rotation);
		mAlwaysChangingPhoneAngle = rotation;

		updateAnimationsState();
		startOrientationSensor();

		reloadMwiCount();

		if(!AsyncProviderLookupOperation.isAsyncTaskRuning&&!providerLookupOperation_executed){
			Log.e("ttt LinphoneActivity AsyncProviderLookupOperation..");
			providerLookupOperation = new AsyncProviderLookupOperation(null, ctx);
			providerLookupOperation.execute();
			providerLookupOperation_executed=true;
		}

		initMore();
	}

	View.OnClickListener moreOptionsListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			hideAnimation();

			switch (v.getId()){
				case R.id.label_linphone_activity_settings:
					selectedTab = 4;
					changeCurrentFragment(FragmentsAvailable.SETTINGS, null);
					break;
				case R.id.label_linphone_activity_resources:
					selectedTab = 4;
					changeCurrentFragment(FragmentsAvailable.CHATLIST, null);
					break;
				case R.id.label_linphone_activity_videomail:
					getSelectedTab();
					videoMail();
					isMoreButtonClicked = false;
					break;
				case R.id.label_linphone_activity_self_preview:

					//Check both preview and selfview settings. I am assuming that when selecting this option, we want to toggle them both.
					previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
					isPreviewEnabled = mPrefs.getBoolean(previewIsEnabledKey, true);

					selfVideoIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_self_view_key);
					isSelfViewEnabled = mPrefs.getBoolean(selfVideoIsEnabledKey, true);

					if (isPreviewEnabled) {
						mPrefs.edit().putBoolean(previewIsEnabledKey, false).commit();
						mPrefs.edit().putBoolean(selfVideoIsEnabledKey, false).commit();
						selfPreviewTextView.setSelected(false);
					} else {
						mPrefs.edit().putBoolean(previewIsEnabledKey, true).commit();
						mPrefs.edit().putBoolean(selfVideoIsEnabledKey, true).commit();
						selfPreviewTextView.setSelected(true);

					}

					isPreviewEnabled = mPrefs.getBoolean(previewIsEnabledKey, true);
					//update dialerfragment view if visible
					if(DialerFragment.instance().isVisible()){
						DialerFragment.instance().initialize_camera();
					}
					showHideMoreOptions();
					more.setSelected(false);
					more.setBackgroundColor(Color.TRANSPARENT);
					isMoreButtonClicked = false;
					break;
			}
		}
	};

	private void videoMail() {
		try {

			LinphoneManager.getInstance().newOutgoingCall(mPrefs.getString(getString(R.string.pref_voice_mail_key), LinphoneManager.getLc().getDefaultProxyConfig().getAddress().asStringUriOnly()), getResources().getString(R.string.main_menu_videomail));
			LinphoneActivity.instance().resetMessageWaitingCount();
			videoMailTextView.setText(getResources().getString(R.string.main_menu_videomail) + " (" + String.valueOf(LinphoneActivity.instance().getMessageWaitingCount()) + ")");
		}
		catch (Throwable e){
			e.printStackTrace();
		}
	}

	private void initMore() {

		mAnimateLayout = (LinearLayout)findViewById(R.id.layout_linphone_activity_more_menu);
		slideInRightToLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_to_left);
		slideOutLeftToRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_left_to_right);

		TextView settingsTextView = (TextView) findViewById(R.id.label_linphone_activity_settings);
		TextView resourcesTextView = (TextView) findViewById(R.id.label_linphone_activity_resources);

		videoMailTextView = (TextView) findViewById(R.id.label_linphone_activity_videomail);
		videoMailTextView.setText(getResources().getString(R.string.main_menu_videomail) + " (" + String.valueOf(LinphoneActivity.instance().getMessageWaitingCount()) + ")");

		selfPreviewTextView = (TextView) findViewById(R.id.label_linphone_activity_self_preview);

		//Check both preview and selfview settings. I am assuming that when selecting this option, we want to toggle them both.
		previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
		isPreviewEnabled = mPrefs.getBoolean(previewIsEnabledKey, true);

		selfVideoIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_self_view_key);
		isSelfViewEnabled = mPrefs.getBoolean(selfVideoIsEnabledKey, true);

		//I'm setting this setting based on preview, since it is on the dialer
		selfPreviewTextView.setSelected(isPreviewEnabled);

		settingsTextView.setOnClickListener(moreOptionsListener);
		resourcesTextView.setOnClickListener(moreOptionsListener);
		videoMailTextView.setOnClickListener(moreOptionsListener);
		selfPreviewTextView.setOnClickListener(moreOptionsListener);
	}

	private void showHideMoreOptions() {
		if (isAnimationDisabled) {
			if (moreMenuVisibility == View.VISIBLE) {
				getSelectedTab();
				hideAnimation();
				moreMenuVisibility = View.GONE;
			} else {
				more.setSelected(true);
				more.setBackgroundColor(getResources().getColor(R.color.orange_background));
				showAnimation();
				moreMenuVisibility = View.VISIBLE;
			}

		} else {
			if (mAnimateLayout.getVisibility() == View.VISIBLE) {
				dialer.setSelected(true);
				dialer.setBackgroundColor(getResources().getColor(R.color.orange_background));
				mAnimateLayout.setVisibility(View.INVISIBLE);
			} else {
				more.setSelected(true);
				more.setBackgroundColor(getResources().getColor(R.color.orange_background));
				mAnimateLayout.setVisibility(View.VISIBLE);
			}
		}
	}

	private void getSelectedTab() {
		switch (selectedTab) {
			case 0:
				dialer.setSelected(true);
				dialer.setBackgroundColor(getResources().getColor(R.color.orange_background));
				break;
			case 1:
				history.setSelected(true);
				history.setBackgroundColor(getResources().getColor(R.color.orange_background));
				break;
			case 2:
				contacts.setSelected(true);
				contacts.setBackgroundColor(getResources().getColor(R.color.orange_background));
				break;
			case 3:
				chat.setSelected(true);
				chat.setBackgroundColor(getResources().getColor(R.color.orange_background));
				break;
			case 4:
				more.setSelected(true);
				more.setBackgroundColor(getResources().getColor(R.color.orange_background));
				break;
		}
	}

	private void showAnimation (){
		slideInRightToLeft.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mAnimateLayout.setVisibility(View.VISIBLE);
				slideInRightToLeft.cancel();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		mAnimateLayout.startAnimation(slideInRightToLeft);
	}

	private void hideAnimation (){
		if (mAnimateLayout.getVisibility() == View.VISIBLE) {
			slideOutLeftToRight.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					mAnimateLayout.setVisibility(View.INVISIBLE);
					slideOutLeftToRight.cancel();
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			mAnimateLayout.startAnimation(slideOutLeftToRight);
		}
	}

	public boolean isVideoMailMessagesViewed()
	{
		boolean res = true;
		if(mPrefs!= null)
			res = mPrefs.getBoolean(UNREAD_VIDEO_MAIL_MESSAGES, true);
		return res;
	}
	public void reloadMwiCount(){

		if (isVideoMailMessagesViewed()) {
			missedVideoMails.setVisibility(View.GONE);
		} else {
			missedVideoMails.setVisibility(View.VISIBLE);
		}

		if (videoMailTextView != null)
			videoMailTextView.setText(getResources().getString(R.string.main_menu_videomail) + " (" + String.valueOf(LinphoneActivity.instance().getMessageWaitingCount()) + ")");

	}
//	private void go_back_to_login(){
//		deleteDefaultAccount();
//		Log.d("Restarting Login because registration failed");
//		Intent intent = new Intent(LinphoneService.instance(), SetupActivity.class);
//		startActivityForResult(intent, FIRST_LOGIN_ACTIVITY);
//	}

	private void deleteDefaultAccount(){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneManager.getInstance().getContext());
		LinphonePreferences mPrefs = LinphonePreferences.instance();
		int n= mPrefs.getDefaultAccountIndex();
		mPrefs.deleteAccount(n);
	}

	public void initButtons(boolean isForce508) {
		menu = (LinearLayout) findViewById(R.id.menu);
		mark = (LinearLayout) findViewById(R.id.mark);

		history = (RelativeLayout) findViewById(R.id.history);
		history.setOnClickListener(this);

		contacts = (RelativeLayout) findViewById(R.id.contacts);
		contacts.setOnClickListener(this);
		dialer = (RelativeLayout) findViewById(R.id.dialer);
		dialer.setOnClickListener(this);
		more = (RelativeLayout) findViewById(R.id.settings);
		more.setOnClickListener(this);
		chat = (RelativeLayout) findViewById(R.id.chat);
		chat.setOnClickListener(this);
		aboutChat = (RelativeLayout) findViewById(R.id.about_chat);

		if (getResources().getBoolean(R.bool.replace_chat_by_about)) {
			chat.setVisibility(View.GONE);
			chat.setOnClickListener(null);
			findViewById(R.id.completeChat).setVisibility(View.GONE);
			aboutChat.setVisibility(View.VISIBLE);
			aboutChat.setOnClickListener(this);
		}
		if (getResources().getBoolean(R.bool.replace_settings_by_about)) {
			more.setVisibility(View.GONE);
			more.setOnClickListener(null);
		}

		missedCalls = (TextView) findViewById(R.id.missedCalls);
		missedChats = (TextView) findViewById(R.id.missedChats);
		missedVideoMails = (TextView) findViewById(R.id.mwi_badge);

		isMessagesViewed = mPrefs.getBoolean(UNREAD_MESSAGES, true);
		if (isMessagesViewed) {
			missedChats.setVisibility(View.GONE);
		} else {
			missedChats.setVisibility(View.VISIBLE);
			missedChats.setText("!");
		}

		if (isVideoMailMessagesViewed()) {
			missedVideoMails.setVisibility(View.GONE);
		} else {
			missedVideoMails.setVisibility(View.VISIBLE);
		}

		setColorTheme(this);
		setBackgroundColorTheme(this);

		TextView historyView = (TextView) history.findViewById(R.id.text);
		TextView contactsView = (TextView) contacts.findViewById(R.id.text);
		TextView dialerView = (TextView) dialer.findViewById(R.id.text);
		TextView resourcesView = (TextView) chat.findViewById(R.id.text);
		TextView settingsView = (TextView) more.findViewById(R.id.text);

		historyView.setTextColor(getResources().getColorStateList(R.color.text_color_selector));
		contactsView.setTextColor(getResources().getColorStateList(R.color.text_color_selector));
		dialerView.setTextColor(getResources().getColorStateList(R.color.text_color_selector));
		resourcesView.setTextColor(getResources().getColorStateList(R.color.text_color_selector));
		settingsView.setTextColor(getResources().getColorStateList(R.color.text_color_selector));

		if (isForce508) {
			historyView.setTextColor(Color.WHITE);
			contactsView.setTextColor(Color.WHITE);
			dialerView.setTextColor(Color.WHITE);
			resourcesView.setTextColor(Color.WHITE);
			settingsView.setTextColor(Color.WHITE);
		}
	}

	public void setColorTheme(Context context){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		int foregroundColor = prefs.getInt(getString(R.string.pref_theme_foreground_color_setting_key), Color.TRANSPARENT);
		((ImageView)history.findViewById(R.id.image)).setColorFilter(foregroundColor);

		((ImageView)contacts.findViewById(R.id.image)).setColorFilter(foregroundColor);
		((ImageView)dialer.findViewById(R.id.image)).setColorFilter(foregroundColor);
		((ImageView)more.findViewById(R.id.image)).setColorFilter(foregroundColor);
		((ImageView)chat.findViewById(R.id.image)).setColorFilter(foregroundColor);
	}

	public void setBackgroundColorTheme(Context context){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int backgroundColor = prefs.getInt(getString(R.string.pref_theme_background_color_setting_key), Color.TRANSPARENT);
		//set background color independent
		if(topLayout!=null) {
			topLayout.setBackgroundColor(backgroundColor);
		}
	}
	public boolean isTablet() {
		return getResources().getBoolean(R.bool.isTablet);
	}

	public void hideStatusBar() {
		if (isTablet()) {
			return;
		}

		findViewById(R.id.status).setVisibility(View.GONE);
		findViewById(R.id.fragmentContainer).setPadding(0, 0, 0, 0);
	}

	public void showStatusBar() {
		if (isTablet()) {
			return;
		}

		if (statusFragment != null && !statusFragment.isVisible()) {
			// Hack to ensure statusFragment is visible after coming back to
			// dialer from chat
			statusFragment.getView().setVisibility(View.VISIBLE);
		}
		findViewById(R.id.status).setVisibility(View.VISIBLE);
		findViewById(R.id.fragmentContainer).setPadding(0, LinphoneUtils.pixelsToDpi(getResources(), 40), 0, 0);
	}

	public void isNewProxyConfig(){
		newProxyConfig = true;
	}

	private void changeCurrentFragment(FragmentsAvailable newFragmentType, Bundle extras) {
		changeCurrentFragment(newFragmentType, extras, false);
	}

	private void changeCurrentFragment(FragmentsAvailable newFragmentType, Bundle extras, boolean withoutAnimation) {

		if(g.analytics_tracker==null){
			g.analytics_tracker=new GAUtils(getApplicationContext()).getInstance(getApplicationContext());
		}
		if (newFragmentType == currentFragment && newFragmentType != FragmentsAvailable.CHAT && newFragmentType != FragmentsAvailable.SETTINGS) {
			return;
		}
		nextFragment = newFragmentType;

		if (currentFragment == FragmentsAvailable.DIALER) {
			try {
				dialerSavedState = getSupportFragmentManager().saveFragmentInstanceState(dialerFragment);
			} catch (Exception e) {
			}
		}

		switch (newFragmentType) {

		case HISTORY:
			//Screen Hit
			//Log.i(Log.TAG, "Setting screen name: Call History Screen");
			g.analytics_tracker.setScreenName("Call History Screen");
			if (getResources().getBoolean(R.bool.use_simple_history)) {
				newFragment = new HistorySimpleFragment();
			} else {
				newFragment = new HistoryFragment();
			}
			break;
		case HISTORY_DETAIL:
			//Log.i(Log.TAG, "Setting screen name: Call History Detail Screen");
			g.analytics_tracker.setScreenName("Call History Detail Screen");
			newFragment = new HistoryDetailFragment();
			break;
		case CONTACTS:
			//Log.i(Log.TAG, "Setting screen name: Contacts Screen");
			g.analytics_tracker.setScreenName("Contacts Screen");
			if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
				Intent i = new Intent();
			    i.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.DialtactsContactsEntryActivity"));
			    i.setAction("android.intent.action.MAIN");
			    i.addCategory("android.intent.category.LAUNCHER");
			    i.addCategory("android.intent.category.DEFAULT");
			    startActivity(i);
			} else {
				newFragment = new ContactsFragment();
				friendStatusListenerFragment = newFragment;
			}
			break;
		case CONTACT:
			//Log.i(Log.TAG, "Setting screen name: View Contact Screen");
			g.analytics_tracker.setScreenName("View Contact Screen");
			newFragment = new ContactFragment();
			break;
		case EDIT_CONTACT:
			//Log.i(Log.TAG, "Setting screen name: Edit Contact Screen");
			g.analytics_tracker.setScreenName("Edit Contact Screen");
			newFragment = new EditContactFragment();
			break;
		case DIALER:
			//Log.i(Log.TAG, "Setting screen name: Dialer Screen");
			g.analytics_tracker.setScreenName("Dialer Screen");
			newFragment = new DialerFragment();
			if (extras == null) {
				newFragment.setInitialSavedState(dialerSavedState);
			}
			dialerFragment = newFragment;
			break;
		case SETTINGS:
			//Log.i(Log.TAG, "Setting screen name: Settings Screen");
			g.analytics_tracker.setScreenName("Settings Screen");
			newFragment = new SettingsFragment();
			break;
		case ACCOUNT_SETTINGS:
			//Log.i(Log.TAG, "Setting screen name: Account Settings Screen");
			g.analytics_tracker.setScreenName("Account Settings Screen");
			newFragment = new AccountPreferencesFragment();
			break;
		case ABOUT:
		case ABOUT_INSTEAD_OF_CHAT:
		case ABOUT_INSTEAD_OF_SETTINGS:
			//Log.i(Log.TAG, "Setting screen name: About Screen");
			g.analytics_tracker.setScreenName("About Screen");
			newFragment = new AboutFragment();
			break;
		case CHATLIST:
			//Log.i(Log.TAG, "Setting screen name: Resources Screen");
			g.analytics_tracker.setScreenName("Resources Screen");
			newFragment = new HelpFragment();
			//messageListFragment = new Fragment();
			break;
		case CHAT:
			//Log.i(Log.TAG, "Setting screen name: Chat Screen (Not in call)");
			g.analytics_tracker.setScreenName("Chat Screen (Not in call)");
			newFragment = new ChatListFragment();
			break;
		default:
			break;

		}

		if (newFragment != null) {
			newFragment.setArguments(extras);
			if (isTablet()) {
				changeFragmentForTablets(newFragment, newFragmentType, withoutAnimation);
			} else {
				changeFragment(newFragment, newFragmentType, withoutAnimation);
			}
		}
	}

	private void updateAnimationsState() {
		isAnimationDisabled = getResources().getBoolean(R.bool.disable_animations) || !LinphonePreferences.instance().areAnimationsEnabled();
	}

	public boolean isAnimationDisabled() {
		return isAnimationDisabled;
	}

	private void changeFragment(Fragment newFragment, FragmentsAvailable newFragmentType, boolean withoutAnimation) {
		if (statusFragment != null) {
			statusFragment.closeStatusBar();
		}

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

//		if (!withoutAnimation && !isAnimationDisabled && currentFragment.shouldAnimate()) {
//			if (newFragmentType.isRightOf(currentFragment)) {
//				transaction.setCustomAnimations(R.anim.slide_in_right_to_left,
//						R.anim.slide_out_right_to_left,
//						R.anim.slide_in_left_to_right,
//						R.anim.slide_out_left_to_right);
//			} else {
//				transaction.setCustomAnimations(R.anim.slide_in_left_to_right,
//						R.anim.slide_out_left_to_right,
//						R.anim.slide_in_right_to_left,
//						R.anim.slide_out_right_to_left);
//			}
//		}

		if (newFragmentType != FragmentsAvailable.DIALER
				|| newFragmentType != FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
				|| newFragmentType != FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
				|| newFragmentType != FragmentsAvailable.CONTACTS
				|| newFragmentType != FragmentsAvailable.CHATLIST
				|| newFragmentType != FragmentsAvailable.HISTORY) {
			transaction.addToBackStack(newFragmentType.toString());
		}
		try
		{
			transaction.replace(R.id.fragmentContainer, newFragment, newFragmentType.toString());
			transaction.commitAllowingStateLoss();
			getSupportFragmentManager().executePendingTransactions();
		}
		catch (Exception ex)
		{
			ex.toString();
		}


		currentFragment = newFragmentType;
	}

	private void changeFragmentForTablets(Fragment newFragment, FragmentsAvailable newFragmentType, boolean withoutAnimation) {
//		if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
//			if (newFragmentType == FragmentsAvailable.DIALER) {
//				showStatusBar();
//			} else {
//				hideStatusBar();
//			}
//		}
		if (statusFragment != null) {
			statusFragment.closeStatusBar();
		}

		LinearLayout ll = (LinearLayout) findViewById(R.id.fragmentContainer2);

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (newFragmentType.shouldAddItselfToTheRightOf(currentFragment)) {
			ll.setVisibility(View.VISIBLE);

			transaction.addToBackStack(newFragmentType.toString());
			transaction.replace(R.id.fragmentContainer2, newFragment);
		} else {
			if (newFragmentType == FragmentsAvailable.DIALER
					|| newFragmentType == FragmentsAvailable.ABOUT
					|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
					|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
					|| newFragmentType == FragmentsAvailable.SETTINGS
					|| newFragmentType == FragmentsAvailable.ACCOUNT_SETTINGS) {
				ll.setVisibility(View.GONE);
			} else {
				ll.setVisibility(View.INVISIBLE);
			}

			if (!withoutAnimation && !isAnimationDisabled && currentFragment.shouldAnimate()) {
				if (newFragmentType.isRightOf(currentFragment)) {
					transaction.setCustomAnimations(R.anim.slide_in_right_to_left, R.anim.slide_out_right_to_left, R.anim.slide_in_left_to_right, R.anim.slide_out_left_to_right);
				} else {
					transaction.setCustomAnimations(R.anim.slide_in_left_to_right, R.anim.slide_out_left_to_right, R.anim.slide_in_right_to_left, R.anim.slide_out_right_to_left);
				}
			}

			transaction.replace(R.id.fragmentContainer, newFragment);
		}
		transaction.commitAllowingStateLoss();
		getSupportFragmentManager().executePendingTransactions();

		currentFragment = newFragmentType;
		if (newFragmentType == FragmentsAvailable.DIALER
				|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
				|| newFragmentType == FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
				|| newFragmentType == FragmentsAvailable.SETTINGS
				|| newFragmentType == FragmentsAvailable.CONTACTS
				|| newFragmentType == FragmentsAvailable.CHATLIST
				|| newFragmentType == FragmentsAvailable.HISTORY) {
			try {
				getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (java.lang.IllegalStateException e) {

			}
		}
		fragmentsHistory.add(currentFragment);
	}

	public void displayHistoryDetail(String sipUri, LinphoneCallLog log) {
		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
		} catch (LinphoneCoreException e) {
			Log.e("Cannot display history details",e);
			return;
		}
		Contact c = ContactsManager.getInstance().findContactWithAddress(getContentResolver(), lAddress);

		String displayName = c != null ? c.getName() : null;
		String pictureUri = c != null && c.getPhotoUri() != null ? c.getPhotoUri().toString() : null;

		String status;
		if (log.getDirection() == CallDirection.Outgoing) {
			status = "Outgoing";
		} else {
			if (log.getStatus() == CallStatus.Missed) {
				status = "Missed";
			} else {
				status = "Incoming";
			}
		}

		String callTime = secondsToDisplayableString(log.getCallDuration());
		String callDate = String.valueOf(log.getTimestamp());

		Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer2);
		if (fragment2 != null && fragment2.isVisible() && currentFragment == FragmentsAvailable.HISTORY_DETAIL) {
			HistoryDetailFragment historyDetailFragment = (HistoryDetailFragment) fragment2;
			historyDetailFragment.changeDisplayedHistory(sipUri, displayName, pictureUri, status, callTime, callDate);
		} else {
			Bundle extras = new Bundle();
			extras.putString("SipUri", sipUri);
			if (displayName != null) {
				extras.putString("DisplayName", displayName);
				extras.putString("PictureUri", pictureUri);
			}
			extras.putString("CallStatus", status);
			extras.putString("CallTime", callTime);
			extras.putString("CallDate", callDate);

			changeCurrentFragment(FragmentsAvailable.HISTORY_DETAIL, extras);
		}
	}

	@SuppressLint("SimpleDateFormat")
	private String secondsToDisplayableString(int secs) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.set(0, 0, 0, 0, 0, secs);
		return dateFormat.format(cal.getTime());
	}

	public void displayContact(Contact contact, boolean chatOnly) {
		Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer2);
		if (fragment2 != null && fragment2.isVisible() && currentFragment == FragmentsAvailable.CONTACT) {
			ContactFragment contactFragment = (ContactFragment) fragment2;
			contactFragment.changeDisplayedContact(contact);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("Contact", contact);
			extras.putBoolean("ChatAddressOnly", chatOnly);
			changeCurrentFragment(FragmentsAvailable.CONTACT, extras);
		}
	}

	public void displayContacts(boolean chatOnly) {
		if (chatOnly) {
			preferLinphoneContacts = true;
		}

		Bundle extras = new Bundle();
		extras.putBoolean("ChatAddressOnly", chatOnly);
		changeCurrentFragment(FragmentsAvailable.CONTACTS, extras);
		preferLinphoneContacts = false;
	}

	public void displayContactsForEdition(String sipAddress) {
		Bundle extras = new Bundle();
		extras.putBoolean("EditOnClick", true);
		extras.putString("SipAddress", sipAddress);
		changeCurrentFragment(FragmentsAvailable.CONTACTS, extras);
	}

	public void displayAbout() {
		changeCurrentFragment(FragmentsAvailable.ABOUT, null);
	}

	public boolean displayChatMessageNotification(String address){
		if(chatFragment != null) {
			if(chatFragment.getSipUri().equals(address)){
				return false;
			}
		}
		return true;
	}

	public void displayChat(String sipUri) {
		if (getResources().getBoolean(R.bool.disable_chat)) {
			return;
		}

		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
		} catch (LinphoneCoreException e) {
			Log.e("Cannot display chat",e);
			return;
		}
		Contact contact = ContactsManager.getInstance().findContactWithAddress(getContentResolver(), lAddress);
		String displayName = contact != null ? contact.getName() : null;

		String pictureUri = null;
		String thumbnailUri = null;
		if(contact != null && contact.getPhotoUri() != null){
			pictureUri = contact.getPhotoUri().toString();
			thumbnailUri = contact.getThumbnailUri().toString();
		}


			Intent intent = new Intent(this, ChatActivity.class);
			intent.putExtra("SipUri", sipUri);
			if (contact != null) {
				intent.putExtra("DisplayName", contact.getName());
				intent.putExtra("PictureUri", pictureUri);
				intent.putExtra("ThumbnailUri", thumbnailUri);
			}
			//
			startOrientationSensor();
			startActivityForResult(intent, CHAT_ACTIVITY);

		//LinphoneService.instance().resetMessageNotifCount();
		//LinphoneService.instance().removeMessageNotification();
		//displayMissedChats(getChatStorage().getUnreadMessageCount());
	}

	public int getMessageWaitingCount(){
		SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		int count = prefs.getInt("mwi_count", 0);
		return count;
	}
	public void resetMessageWaitingCount(){
		SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		prefs.edit().putInt("mwi_count", 0).commit();
		mPrefs.edit().putBoolean(UNREAD_VIDEO_MAIL_MESSAGES, true).commit();
		reloadMwiCount();
	}
	@Override
	public void onClick(View v) {
		int id = v.getId();
		resetSelection();
		if (isAnimationDisabled) {
			hideAnimation();
		} else {
			if (mAnimateLayout.getVisibility() == View.VISIBLE) {
				mAnimateLayout.setVisibility(View.INVISIBLE);
			}
		}
		if (id == R.id.history) {
			selectedTab = 1;
			changeCurrentFragment(FragmentsAvailable.HISTORY, null);
			history.setSelected(true);
			history.setBackgroundColor(getResources().getColor(R.color.orange_background));
			getLc().resetMissedCallsCount();
			displayMissedCalls(0);
			isMoreButtonClicked = false;
		} else if (id == R.id.contacts) {
			selectedTab = 2;
			changeCurrentFragment(FragmentsAvailable.CONTACTS, null);
			contacts.setSelected(true);
			contacts.setBackgroundColor(getResources().getColor(R.color.orange_background));
			if(!ApplicationPermissionManager.isPermissionGranted(this, Manifest.permission.WRITE_CONTACTS))
			{
				ApplicationPermissionManager.askPermission(this, Manifest.permission.WRITE_CONTACTS, REQUEST_CONTACTS_PERMISSION);

			}
			isMoreButtonClicked = false;
		} else if (id == R.id.dialer) {
			selectedTab = 0;
			changeCurrentFragment(FragmentsAvailable.DIALER, null);
			if(!isTablet()) {
				if(DialerFragment.instance() != null) {
					if (DialerFragment.instance().VIEW_INDEX == DialerFragment.instance().DIALER_INDEX) {
						DialerFragment.instance().dialer_content.setVisibility(View.VISIBLE);
						DialerFragment.instance().VIEW_INDEX = DialerFragment.instance().SELF_VIEW_INDEX;
					} else {
						DialerFragment.instance().dialer_content.setVisibility(View.GONE);
						DialerFragment.instance().VIEW_INDEX = DialerFragment.instance().DIALER_INDEX;
					}
				}
			}
			dialer.setSelected(true);
			dialer.setBackgroundColor(getResources().getColor(R.color.orange_background));
			isMoreButtonClicked = false;
		} else if (id == R.id.settings) {
			showHideMoreOptions();

			if (isMoreButtonClicked)
				isMoreButtonClicked = false;
			else isMoreButtonClicked = true;

			missedVideoMails.setVisibility(View.GONE);
			if(!isVideoMailMessagesViewed())
				mPrefs.edit().putBoolean(UNREAD_VIDEO_MAIL_MESSAGES, true).commit();

		} else if (id == R.id.about_chat) {
			Bundle b = new Bundle();
			b.putSerializable("About", FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT);
			changeCurrentFragment(FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT, b);
			aboutChat.setSelected(true);
			aboutChat.setBackgroundColor(getResources().getColor(R.color.orange_background));
			isMoreButtonClicked = false;
		} else if (id == R.id.chat) {
			selectedTab = 3;
			changeCurrentFragment(FragmentsAvailable.CHAT, null);
			chat.setSelected(true);
			chat.setBackgroundColor(getResources().getColor(R.color.orange_background));
			isMessagesViewed = true;
			missedChats.setVisibility(View.GONE);
			mPrefs.edit().putBoolean(UNREAD_MESSAGES, isMessagesViewed).commit();
			isMoreButtonClicked = false;
		}
	}

	private void resetSelection() {
		history.setSelected(false);
		history.setBackgroundColor(Color.TRANSPARENT);
		contacts.setSelected(false);
		contacts.setBackgroundColor(Color.TRANSPARENT);
		dialer.setSelected(false);
		dialer.setBackgroundColor(Color.TRANSPARENT);
		more.setSelected(false);
		more.setBackgroundColor(Color.TRANSPARENT);
		chat.setSelected(false);
		chat.setBackgroundColor(Color.TRANSPARENT);
		aboutChat.setSelected(false);
		aboutChat.setBackgroundColor(Color.TRANSPARENT);
	}

	@SuppressWarnings("incomplete-switch")
	public void selectMenu(FragmentsAvailable menuToSelect) {
		currentFragment = menuToSelect;

		switch (menuToSelect) {
			case HISTORY:
			case HISTORY_DETAIL:
				history.setSelected(true);
				break;
			case CONTACTS:
			case CONTACT:
			case EDIT_CONTACT:
				contacts.setSelected(true);
				break;
			case DIALER:
				if (!isMoreButtonClicked) {
					resetSelection();
					dialer.setSelected(true);
					dialer.setBackgroundColor(getResources().getColor(R.color.orange_background));
				}
				break;
			case SETTINGS:
			case ACCOUNT_SETTINGS:
				more.setSelected(true);
				break;
			case ABOUT_INSTEAD_OF_CHAT:
				aboutChat.setSelected(true);
				break;
			case CHATLIST:
			case CHAT:
				chat.setSelected(true);
				break;
		}
	}

	public void updateDialerFragment(DialerFragment fragment) {
		dialerFragment = fragment;
		// Hack to maintain soft input flags
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	public void updateChatFragment(ChatFragment fragment) {
		chatFragment = fragment;
	}

	public void updateChatListFragment(ChatListFragment fragment) {
		messageListFragment = fragment;
	}

	public void hideMenu(boolean hide) {
		menu.setVisibility(hide ? View.GONE : View.VISIBLE);
		mark.setVisibility(hide ? View.GONE : View.VISIBLE);
	}

	public void updateStatusFragment(StatusFragment fragment) {
		statusFragment = fragment;
	}

	public void refreshFragment(FragmentsAvailable fragment, RelativeLayout tab_to_show_selected){
		changeCurrentFragment(fragment, null);
		tab_to_show_selected.setSelected(true);
	}

	public void displaySettings() {
		changeCurrentFragment(FragmentsAvailable.SETTINGS, null);
		more.setSelected(true);
	}

	public void applyConfigChangesIfNeeded() {
		if (nextFragment != FragmentsAvailable.SETTINGS && nextFragment != FragmentsAvailable.ACCOUNT_SETTINGS) {
			updateAnimationsState();
		}
	}

	public void displayAccountSettings(int accountNumber) {
		Bundle bundle = new Bundle();
		bundle.putInt("Account", accountNumber);
		changeCurrentFragment(FragmentsAvailable.ACCOUNT_SETTINGS, bundle);
		more.setSelected(true);
	}

	public StatusFragment getStatusFragment() {
		return statusFragment;
	}

	public List<String> getChatList() {
		return getChatStorage().getChatList();
	}

	public List<String> getDraftChatList() {
		return getChatStorage().getDrafts();
	}

	public List<ChatMessage> getChatMessages(String correspondent) {
		return getChatStorage().getMessages(correspondent);
	}

	public void removeFromChatList(String sipUri) {
		getChatStorage().removeDiscussion(sipUri);
	}

	public void removeFromDrafts(String sipUri) {
		getChatStorage().deleteDraft(sipUri);
	}



	public void updateMissedChatCount(boolean isNewMessage) {
		displayMissedChats(getChatStorage().getUnreadMessageCount(), isNewMessage);
	}

	public int onMessageSent(String to, String message) {
		getChatStorage().deleteDraft(to);
		if (messageListFragment != null && messageListFragment.isVisible()) {
			((ChatListFragment) messageListFragment).refresh();
		}

		return getChatStorage().saveTextMessage("", to, message, System.currentTimeMillis());
	}

	public int onMessageSent(String to, Bitmap image, String imageURL) {
		getChatStorage().deleteDraft(to);
		return getChatStorage().saveImageMessage("", to, image, imageURL, System.currentTimeMillis());
	}

	public void onMessageStateChanged(String to, String message, int newState) {
		getChatStorage().updateMessageStatus(to, message, newState);
	}

	public void onImageMessageStateChanged(String to, int id, int newState) {
		getChatStorage().updateMessageStatus(to, id, newState);
	}

	private void displayMissedCalls(final int missedCallsCount) {
		if (missedCallsCount > 0) {
			missedCalls.setText(missedCallsCount + "");
			missedCalls.setVisibility(View.VISIBLE);
			if (!isAnimationDisabled) {
				missedCalls.startAnimation(AnimationUtils.loadAnimation(LinphoneActivity.this, R.anim.bounce));
			}
		} else {
			missedCalls.clearAnimation();
			missedCalls.setVisibility(View.GONE);
		}
	}

	public void unreadMessageBadge(){
		isMessagesViewed = true;
		missedChats.setVisibility(View.GONE);
		mPrefs.edit().putBoolean(UNREAD_MESSAGES, isMessagesViewed).commit();
	}
	private void displayMissedChats(final int missedChatCount, final boolean isNewMessage) {

		if (isNewMessage) {
			isMessagesViewed = false;
			if (missedChatCount > 0) {
				missedChats.setText("!");
				missedChats.setVisibility(View.VISIBLE);
				if (!isAnimationDisabled) {
					missedChats.startAnimation(AnimationUtils.loadAnimation(LinphoneActivity.this, R.anim.bounce));
				}
			} else {
				missedChats.clearAnimation();
				missedChats.setVisibility(View.GONE);
			}
		}
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

	@Override
	public void setAddresGoToDialerAndCall(String number, String name, Uri photo) {
//		Bundle extras = new Bundle();
//		extras.putString("SipUri", number);
//		extras.putString("DisplayName", name);
//		extras.putString("Photo", photo == null ? null : photo.toString());
//		changeCurrentFragment(FragmentsAvailable.DIALER, extras);

		AddressType address = new AddressText(this, null);
		address.setDisplayedName(name);
		address.setText(number);
		LinphoneManager.getInstance().newOutgoingCall(address);
	}

	public void setAddressAndGoToDialer(String number) {
		Bundle extras = new Bundle();
		extras.putString("SipUri", number);
		changeCurrentFragment(FragmentsAvailable.DIALER, extras);
	}

	@Override
	public void goToDialer() {
		changeCurrentFragment(FragmentsAvailable.DIALER, null);
	}

	public void startVideoActivity(LinphoneCall currentCall) {
		Intent intent = new Intent(this, InCallActivity.class);
		intent.putExtra("VideoEnabled", true);
		//startOrientationSensor();
		startActivityForResult(intent, CALL_ACTIVITY);
	}

	public void startIncallActivity(LinphoneCall currentCall) {
		Intent intent = new Intent(this, InCallActivity.class);
		intent.putExtra("VideoEnabled", false);
		//startOrientationSensor();
		startActivityForResult(intent, CALL_ACTIVITY);
	}

	public void sendLogs(Context context, String info){
		final String appName = context.getString(R.string.app_name);

		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_EMAIL, new String[]{ context.getString(R.string.about_bugreport_email) });
		i.putExtra(Intent.EXTRA_SUBJECT, appName + " Logs");
		i.putExtra(Intent.EXTRA_TEXT, info);
		i.setType("application/zip");

		try {
			startActivity(Intent.createChooser(i, "Send mail..."));
		} catch (android.content.ActivityNotFoundException ex) {
			Log.e(ex);
		}
	}

	/**
	 * Register a sensor to track phoneOrientation changes
	 */
	private synchronized void startOrientationSensor() {
		if (mOrientationHelper == null) {
			mOrientationHelper = new LocalOrientationEventListener(this);
		}
		mOrientationHelper.enable();
	}

	public int mAlwaysChangingPhoneAngle = -1;
	public int lastDeviceAngle = 0;
	public int getDeviceOrientation(){
		return lastDeviceAngle;
	}

	private class LocalOrientationEventListener extends OrientationEventListener {
		public LocalOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(final int o) {
			if (o == OrientationEventListener.ORIENTATION_UNKNOWN) {
				return;
			}

			int degrees = 270;
			degrees = lastDeviceAngle;

			int sensativity = 10;


			if (o < 0 + sensativity || o > 360 - sensativity)// when o is around 0, we set degrees to zero
				degrees = 0;
			else if (o > 90 - sensativity && o < 90 + sensativity)//when o is around 90, we set the degrees to 90
				degrees = 90;
			else if (o > 180 - sensativity && o < 180 + sensativity)//when o is around 180 we set the degrees to 180
				degrees = 180;
			else if (o > 270 - sensativity && o < 270 + sensativity)//when o is around 180 we set the degrees to 180
				degrees = 270;

			if (mAlwaysChangingPhoneAngle == degrees) {
				return;
			}
			mAlwaysChangingPhoneAngle = degrees;

			Log.d("Phone orientation changed to ", degrees);
			lastDeviceAngle = degrees;
			int rotation = (360 - degrees) % 360;
			LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
			if (lc != null) {
				lc.setDeviceRotation(rotation);
				LinphoneCall currentCall = lc.getCurrentCall();
				if (currentCall != null && currentCall.cameraEnabled() && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
					lc.updateCall(currentCall, null);
				}
			}
		}
	}

	private void initInCallMenuLayout(boolean callTransfer) {
		selectMenu(FragmentsAvailable.DIALER);
		if (dialerFragment != null) {
			((DialerFragment) dialerFragment).resetLayout(callTransfer);
		}
	}

	public void resetClassicMenuLayoutAndGoBackToCallIfStillRunning() {
		if (dialerFragment != null) {
			((DialerFragment) dialerFragment).resetLayout(false);
		}

		if (LinphoneManager.isInstanciated() && getLc().getCallsNb() > 0) {
			LinphoneCall call = getLc().getCalls()[0];
			if (call.getState() == LinphoneCall.State.IncomingReceived) {
				LinphoneManager.startIncomingCallActivity(this);
				//startActivity(new Intent(LinphoneActivity.this, IncomingCallActivity.class));
			} else if (call.getCurrentParamsCopy().getVideoEnabled()) {
				startVideoActivity(call);
			} else {
				startIncallActivity(call);
			}
		}
	}

	public FragmentsAvailable getCurrentFragment() {
		return currentFragment;
	}

	public ChatStorage getChatStorage() {
		return ChatStorage.getInstance();
	}

	public void addContact(String displayName, String sipUri)
	{
		if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
			Intent intent = Compatibility.prepareAddContactIntent(displayName, sipUri);
			startActivity(intent);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("NewSipAdress", sipUri);
			changeCurrentFragment(FragmentsAvailable.EDIT_CONTACT, extras);
		}
	}

	public void editContact(Contact contact)
	{
		if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
			Intent intent = Compatibility.prepareEditContactIntent(Integer.parseInt(contact.getID()));
			startActivity(intent);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("Contact", contact);
			changeCurrentFragment(FragmentsAvailable.EDIT_CONTACT, extras);
		}

	}

	public void editContact(Contact contact, String sipAddress)
	{
		if (getResources().getBoolean(R.bool.use_android_native_contact_edit_interface)) {
			Intent intent = Compatibility.prepareEditContactIntentWithSipAddress(Integer.parseInt(contact.getID()), sipAddress);
			startActivity(intent);
		} else {
			Bundle extras = new Bundle();
			extras.putSerializable("Contact", contact);
			extras.putSerializable("NewSipAdress", sipAddress);
			changeCurrentFragment(FragmentsAvailable.EDIT_CONTACT, extras);
		}
	}

	public void exit() {
		stopService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= Build.VERSION_CODES.JELLY_BEAN){
			exitApi16();
		} else{
			finish();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void exitApi16(){
		finishAffinity();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==WIFI_ACTIVITY_RESULT){
			Intent refresh = new Intent(this, LinphoneActivity.class);
			startActivity(refresh);
			this.finish();
		}

		if (resultCode == Activity.RESULT_OK && requestCode == FIRST_LOGIN_ACTIVITY) {
			if (data != null && data.getExtras()!= null ) {
				if(data.getExtras().getBoolean("Exit", false)) {
					exit();
					return;
				}
				else if(data.hasExtra(SetupActivity.AUTO_CONFIG_SUCCED_EXTRA)) {
					String message = data.getExtras().getBoolean(SetupActivity.AUTO_CONFIG_SUCCED_EXTRA, false) ? "Configuration Loaded Successfully" : "Configuration Not Loaded";
//					new AlertDialog.Builder(LinphoneActivity.instance())
//							.setMessage(message)
//							.setTitle("Auto-Configuration")
//							.setPositiveButton("OK", null)
//							.show();
					Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
					toast.show();
				}
			}
		}
		if (resultCode == Activity.RESULT_FIRST_USER && requestCode == SETTINGS_ACTIVITY) {
			if (data.getExtras().getBoolean("Exit", false)) {
				exit();
			} else {
				FragmentsAvailable newFragment = (FragmentsAvailable) data.getExtras().getSerializable("FragmentToDisplay");
				changeCurrentFragment(newFragment, null, true);
				selectMenu(newFragment);
			}
		} else if (resultCode == Activity.RESULT_FIRST_USER && requestCode == CALL_ACTIVITY) {
			getIntent().putExtra("PreviousActivity", CALL_ACTIVITY);
			boolean callTransfer = data == null ? false : data.getBooleanExtra("Transfer", false);
			if (getLc().getCallsNb() > 0) {
				initInCallMenuLayout(callTransfer);
			} else {
				resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}



	@Override
	protected void onPause() {
		getIntent().putExtra("PreviousActivity", 0);
		super.onPause();
		unregisterManagers();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		if (LinphonePreferences.instance().getAccountCount() == 0) {
//			startActivityForResult(new Intent().setClass(LinphoneActivity.this, SetupActivity.class), FIRST_LOGIN_ACTIVITY);
//		}
		// Attempt to update user location
		Utils.check_network_status(this, WIFI_ACTIVITY_RESULT);//Anytime activity is resumed and we don't have internet, tell the user.. and offer them to turn on wifi.
		try {
			boolean hasGps = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
			if (hasGps) {
				LinphoneLocationManager.instance(this).updateLocation();
			} else {
				Log.d("TAG", "NO GPS");
			}
		}
		catch(NullPointerException e){
			Log.e("E", "Device does not have GPS support");
		}
		if (!LinphoneService.isReady())  {
			startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
		}

		ContactsManager.getInstance().prepareContactsInBackground();

		updateMissedChatCount(false);
		LinphoneActivity.instance().reloadMwiCount();
		displayMissedCalls(getLc().getMissedCallsCount());
		LinphoneManager.getInstance().changeStatusToOnline();

		if(getIntent().getIntExtra("PreviousActivity", 0) != CALL_ACTIVITY){
			if (getLc().getCalls().length > 0) {
				LinphoneCall call = getLc().getCalls()[0];
				LinphoneCall.State callState = call.getState();
				if (callState == State.IncomingReceived) {
					LinphoneManager.startIncomingCallActivity(this);
					//startActivity(new Intent(this, IncomingCallActivity.class));
				} else {

					if (call.getCurrentParamsCopy().getVideoEnabled()) {
						startVideoActivity(call);
					} else {
						startIncallActivity(call);
					}
				}
			}
		}

		if(BuildConfig.DEBUG) {
			//Debug
			checkForCrashes();
		} else{
			//Release
			checkForCrashes();
		}
	}

	@Override
	protected void onDestroy() {
		unregisterManagers();
		if (mOrientationHelper != null) {
			mOrientationHelper.disable();
			mOrientationHelper = null;
		}

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.terminateAllCalls();
			lc.removeListener(mListener);
		}

		instance = null;
		super.onDestroy();

		unbindDrawables(findViewById(R.id.topLayout));
		System.gc();
	}

	private void unbindDrawables(View view) {
		if (view != null && view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	public void showMessageFromNotification(Intent intent)
	{
		try {
			Bundle extras = intent.getExtras();
			LinphoneService.instance().removeMessageNotification();
			String sipUri = extras.getString("ChatContactSipUri");
			displayChat(sipUri);
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if (extras != null && extras.getBoolean("GoToChat", false)) {
			showMessageFromNotification(intent);

		} else if (extras != null && extras.getBoolean("Notification", false)) {
			if (getLc().getCallsNb() > 0) {
				LinphoneCall call = getLc().getCalls()[0];
				if (call.getCurrentParamsCopy().getVideoEnabled()) {
					startVideoActivity(call);
				} else {
					startIncallActivity(call);
				}
			}
		} else {
			if (dialerFragment != null) {
				if (extras != null && extras.containsKey("SipUriOrNumber")) {
					if (getResources().getBoolean(R.bool.automatically_start_intercepted_outgoing_gsm_call)) {
						((DialerFragment) dialerFragment).newOutgoingCall(extras.getString("SipUriOrNumber"));
					} else {
						((DialerFragment) dialerFragment).displayTextInAddressBar(extras.getString("SipUriOrNumber"));
					}
				} else {
					((DialerFragment) dialerFragment).newOutgoingCall(intent);
				}
			}
			if (getLc().getCalls().length > 0) {
				LinphoneCall calls[] = getLc().getCalls();
				if (calls.length > 0) {
					LinphoneCall call = calls[0];

					if (call != null && call.getState() != LinphoneCall.State.IncomingReceived) {
						if (call.getCurrentParamsCopy().getVideoEnabled()) {
							startVideoActivity(call);
						} else {
							startIncallActivity(call);
						}
					}
				}

				// If a call is ringing, start incomingcallactivity
				Collection<LinphoneCall.State> incoming = new ArrayList<LinphoneCall.State>();
				incoming.add(LinphoneCall.State.IncomingReceived);
				if (LinphoneUtils.getCallsInState(getLc(), incoming).size() > 0) {
					if (InCallActivity.isInstanciated()) {
						LinphoneManager.startIncomingCallActivity(this);
						//InCallActivity.instance().startIncomingCallActivity();
					} else {
						LinphoneManager.startIncomingCallActivity(this);
						//startActivity(new Intent(this, IncomingCallActivity.class));
					}
				}
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (currentFragment == FragmentsAvailable.DIALER
					|| currentFragment == FragmentsAvailable.CONTACTS
					|| currentFragment == FragmentsAvailable.HISTORY
					|| currentFragment == FragmentsAvailable.CHATLIST
					|| currentFragment == FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT
					|| currentFragment == FragmentsAvailable.ABOUT_INSTEAD_OF_SETTINGS
					|| currentFragment == FragmentsAvailable.SETTINGS
					|| currentFragment == FragmentsAvailable.ABOUT) {

				if(currentFragment == FragmentsAvailable.DIALER) {
					boolean isBackgroundModeActive = LinphonePreferences.instance().isBackgroundModeEnabled();
					if (!isBackgroundModeActive) {
						stopService(new Intent(Intent.ACTION_MAIN).setClass(this, LinphoneService.class));
						finish();
					} else if (LinphoneUtils.onKeyBackGoHome(this, keyCode, event)) {
						return true;
					}
				}
				else{
					goToDialer();
					return true;
				}
//			} else {
//				if (isTablet()) {
//					if (currentFragment == FragmentsAvailable.SETTINGS) {
//						updateAnimationsState();
//					}
//				}
			}
		} else if (keyCode == KeyEvent.KEYCODE_MENU && statusFragment != null) {
			if (event.getRepeatCount() < 1) {
				statusFragment.openOrCloseStatusBar(true);
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	private void checkForCrashes() {

		CrashManager.register(this, "d6280d4d277d6876c709f4143964f0dc",new CrashManagerListener() {
			public boolean shouldAutoUploadCrashes() {
				return true;
			}
			public boolean ignoreDefaultHandler() {
				return true;
			}
			public String getContact() {
				LinphonePreferences mPrefs = LinphonePreferences.instance();
				String username = mPrefs.getAccountUsername(mPrefs.getDefaultAccountIndex());
				String domain = mPrefs.getAccountDomain(mPrefs.getDefaultAccountIndex());
				String user=username + "@" + domain;
				return user;
			}
			public String getDescription() {
				String description = "";

				try {
					Process process = Runtime.getRuntime().exec("logcat -d HockeyApp:D *:S");
					BufferedReader bufferedReader =
							new BufferedReader(new InputStreamReader(process.getInputStream()));

					StringBuilder log = new StringBuilder();
					String line;
					while ((line = bufferedReader.readLine()) != null) {
						log.append(line);
						log.append(System.getProperty("line.separator"));
					}
					bufferedReader.close();

					description = log.toString();
				}
				catch (IOException e) {
				}

				File crashStacktrace = null;
				try
				{
					File root = new File(Environment.getExternalStorageDirectory(), "ACE");
					if (!root.exists()) {
						root.mkdirs();
					}
					crashStacktrace = new File(root, "hockeyAppCrashFeedback.txt");
					FileWriter writer = new FileWriter(crashStacktrace);
					writer.append(description);
					writer.flush();
					writer.close();
					Toast.makeText(LinphoneActivity.ctx, "Crash feedback saved", Toast.LENGTH_SHORT).show();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				android.util.Log.e("Info", "Uri.fromFile(Crash feedback) = " + Uri.fromFile(crashStacktrace));


				return description;
			}
			public String getUserID() {
				return Settings.Secure.getString(getContentResolver(),
						Settings.Secure.ANDROID_ID);
			}
			public void onCrashesSent() {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(act, "Crash data was sent. Thanks!", Toast.LENGTH_LONG).show();
					}
				});

			}
			public void onCrashesNotSent() {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(act, "Crash data failed to sent. Please try again later.", Toast.LENGTH_LONG).show();
					}
				});

			}
		});
	}

	private void checkForUpdates() {
		// Remove this for store / production builds!
		UpdateManager.register(this, "d6280d4d277d6876c709f4143964f0dc");
	}
	private void unregisterManagers() {
		UpdateManager.unregister();
		// unregister other managers if necessary...
	}




	void onPermissionGrandted(int permission_code)
	{
		Fragment fragment2 = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer2);
		if(permission_code==REQUEST_CONTACTS_PERMISSION && currentFragment == FragmentsAvailable.CONTACTS && fragment2 instanceof ContactsFragment)
		{
			//((ContactsFragment)fragment2).invalidate();
		}
		//if contacts and currentfragment is contact
		//reload contacts



	}

	public final static int REQUEST_CAMERA_PERMISSION = 1;
	public final static int REQUEST_CONTACTS_PERMISSION = 2;
	public final static int REQUEST_STORAGE_PERMISSION = 3;
	public final static int REQUEST_MIC_PERMISSION = 4;

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults)
	{
		Log.d("permission result jan");
		if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			onPermissionGrandted(requestCode);
		}


	}

	public ArrayList<String> display_all_core_values(LinphoneCore lc, final String filename){
		ArrayList<String> stats_list = new ArrayList<String>();
		try {


			final String LinphoneCoreStatsTitle = "--------Linphone Core Stats---------";
			final String audioMulticastEnabled = LC_Object_to_String(lc.audioMulticastEnabled());
			final String videoMulticastEnabled = LC_Object_to_String(lc.videoMulticastEnabled());
			final String chatEnabled = LC_Object_to_String(lc.chatEnabled());
			final String dnsSrvEnabled = LC_Object_to_String(lc.dnsSrvEnabled());
			final String hasBuiltInEchoCanceler = LC_Object_to_String(lc.hasBuiltInEchoCanceler());
			final String isAdaptiveRateControlEnabled = LC_Object_to_String(lc.isAdaptiveRateControlEnabled());
			final String isEchoCancellationEnabled = LC_Object_to_String(lc.isEchoCancellationEnabled());
			final String isEchoLimiterEnabled = LC_Object_to_String(lc.isEchoLimiterEnabled());
			final String isIncall = LC_Object_to_String(lc.isIncall());
			final String isInComingInvitePending = LC_Object_to_String(lc.isInComingInvitePending());
			final String isIpv6Enabled = LC_Object_to_String(lc.isIpv6Enabled());
			final String isMediaEncryptionMandatory = LC_Object_to_String(lc.isMediaEncryptionMandatory());
			final String isKeepAliveEnabled = LC_Object_to_String(lc.isKeepAliveEnabled());
			final String isMicMuted = LC_Object_to_String(lc.isMicMuted());
			final String isNetworkReachable = LC_Object_to_String(lc.isNetworkReachable());
			final String isSdp200AckEnabled = LC_Object_to_String(lc.isSdp200AckEnabled());
			final String isSpeakerEnabled = LC_Object_to_String(lc.isSpeakerEnabled());
			final String isVideoSupported = LC_Object_to_String(lc.isVideoSupported());
			final String needsEchoCalibration = LC_Object_to_String(lc.needsEchoCalibration());
			final String soundResourcesLocked = LC_Object_to_String(lc.soundResourcesLocked());
			final String upnpAvailable = LC_Object_to_String(lc.upnpAvailable());

			final String getAdaptiveRateAlgorithm = LC_Object_to_String(lc.getAdaptiveRateAlgorithm());
			final String getAudioCodecs = LC_Object_to_String(lc.getAudioCodecs());
			final String getAudioDscp = LC_Object_to_String(lc.getAudioDscp());
			final String getAudioMulticastAddr = LC_Object_to_String(lc.getAudioMulticastAddr());
			final String getAudioMulticastTtl = LC_Object_to_String(lc.getAudioMulticastTtl());
			final String getAuthInfosList = LC_Object_to_String(lc.getAuthInfosList());
			final String getCallLogs = LC_Object_to_String(lc.getCallLogs());
			final String getCalls = LC_Object_to_String(lc.getCalls());
			final String getCallsNb = LC_Object_to_String(lc.getCallsNb());
			//final String getChatRoom=LC_Object_to_String(lc.getChatRoom());
			final String getConference = LC_Object_to_String(lc.getConference());
			final String getConferenceSize = LC_Object_to_String(lc.getConferenceSize());
			final String getConfig = LC_Object_to_String(lc.getConfig());
			final String getCurrentCall = LC_Object_to_String(lc.getCurrentCall());
			final String getDefaultProxyConfig = LC_Object_to_String(lc.getDefaultProxyConfig());
			final String getDownloadBandwidth = LC_Object_to_String(lc.getDownloadBandwidth());
			final String getFileTransferServer = LC_Object_to_String(lc.getFileTransferServer());
			final String getFirewallPolicy = LC_Object_to_String(lc.getFirewallPolicy());
			final String getFriendList = LC_Object_to_String(lc.getFriendList());
			final String getGlobalState = LC_Object_to_String(lc.getGlobalState());
			final String getHttpProxyHost = LC_Object_to_String(lc.getHttpProxyHost());
			final String getHttpProxyPort = LC_Object_to_String(lc.getHttpProxyPort());
			//final String getLastOutgoingCallLog=LC_Object_to_String(lc.getLastOutgoingCallLog());
			final String getLastOutgoingCallLog = "CORRUPT";
			final String getMaxCalls = LC_Object_to_String(lc.getMaxCalls());
			final String getMediaEncryption = LC_Object_to_String(lc.getMediaEncryption());
			final String getMissedCallsCount = LC_Object_to_String(lc.getMissedCallsCount());
			final String getMtu = LC_Object_to_String(lc.getMtu());
			final String getNortpTimeout = LC_Object_to_String(lc.getNortpTimeout());
			//final String getOrCreateChatRoom=LC_Object_to_String(lc.getOrCreateChatRoom());
			//final String getPayloadTypeBitrate=LC_Object_to_String(lc.getPayloadTypeBitrate());
			//final String getPayloadTypeNumber=LC_Object_to_String(lc.getPayloadTypeNumber());
			final String getPlaybackGain = LC_Object_to_String(lc.getPlaybackGain());
			final String getPlayLevel = LC_Object_to_String(lc.getPlayLevel());
			final String getPreferredFramerate = LC_Object_to_String(lc.getPreferredFramerate());
			final String getPreferredVideoSize = LC_Object_to_String(lc.getPreferredVideoSize());
			final String getPresenceModel = LC_Object_to_String(lc.getPresenceModel());
			final String getPrimaryContact = LC_Object_to_String(lc.getPrimaryContact());
			final String getPrimaryContactDisplayName = LC_Object_to_String(lc.getPrimaryContactDisplayName());
			final String getProvisioningUri = LC_Object_to_String(lc.getProvisioningUri());
			final String getProxyConfigList = LC_Object_to_String(lc.getProxyConfigList());
			final String getRemoteAddress = LC_Object_to_String(lc.getRemoteAddress());
			final String getRemoteRingbackTone = LC_Object_to_String(lc.getRemoteRingbackTone());
			final String getRing = LC_Object_to_String(lc.getRing());
			final String getSignalingTransportPorts = LC_Object_to_String(lc.getSignalingTransportPorts());
			final String getSipDscp = LC_Object_to_String(lc.getSipDscp());
			final String getSipTransportTimeout = LC_Object_to_String(lc.getSipTransportTimeout());
			final String getStunServer = LC_Object_to_String(lc.getStunServer());
			final String getSupportedVideoSizes = LC_Object_to_String(lc.getSupportedVideoSizes());
			final String getUploadBandwidth = LC_Object_to_String(lc.getUploadBandwidth());
			final String getUpnpExternalIpaddress = LC_Object_to_String(lc.getUpnpExternalIpaddress());
			final String getUpnpState = LC_Object_to_String(lc.getUpnpState());
			final String getUseRfc2833ForDtmfs = LC_Object_to_String(lc.getUseRfc2833ForDtmfs());
			final String getUseSipInfoForDtmfs = LC_Object_to_String(lc.getUseSipInfoForDtmfs());
			final String getVersion = LC_Object_to_String(lc.getVersion());
			final String getVideoAutoAcceptPolicy = LC_Object_to_String(lc.getVideoAutoAcceptPolicy());
			final String getVideoAutoInitiatePolicy = LC_Object_to_String(lc.getVideoAutoInitiatePolicy());
			final String getVideoCodecs = LC_Object_to_String(lc.getVideoCodecs());
			final String getVideoDevice = LC_Object_to_String(lc.getVideoDevice());
			final String getVideoDscp = LC_Object_to_String(lc.getVideoDscp());
			final String getVideoMulticastAddr = LC_Object_to_String(lc.getVideoMulticastAddr());
			final String getVideoMulticastTtl = LC_Object_to_String(lc.getVideoMulticastTtl());
			final String getVideoPreset = LC_Object_to_String(lc.getVideoPreset());
			final String getPresenceInfo = LC_Object_to_String(lc.getPresenceInfo());
			final String isTunnelAvailable = LC_Object_to_String(lc.isTunnelAvailable());
			final String tunnelGetMode = LC_Object_to_String(lc.tunnelGetMode());
			final String tunnelGetServers = LC_Object_to_String(lc.tunnelGetServers());
			final String tunnelSipEnabled = LC_Object_to_String(lc.tunnelSipEnabled());
			final boolean HWAcellDecode = lc.getMSFactory().filterFromNameEnabled("MSMediaCodecH264Dec");
			final boolean HWAcellEncode = lc.getMSFactory().filterFromNameEnabled("MSMediaCodecH264Enc");
			final String CameraParameters = "--------Device Camera Stats---------";


			//Display to developer
			Log.d("LinphoneCoreStatsTitle,", LinphoneCoreStatsTitle);
			Log.d("audioMulticastEnabled,", audioMulticastEnabled);
			Log.d("videoMulticastEnabled,", videoMulticastEnabled);
			Log.d("chatEnabled,", chatEnabled);
			Log.d("dnsSrvEnabled,", dnsSrvEnabled);
			Log.d("hasBuiltInEchoCanceler,", hasBuiltInEchoCanceler);
			Log.d("isAdaptiveRateControlEnabled,", isAdaptiveRateControlEnabled);
			Log.d("isEchoCancellationEnabled,", isEchoCancellationEnabled);
			Log.d("isEchoLimiterEnabled,", isEchoLimiterEnabled);
			Log.d("isIncall,", isIncall);
			Log.d("isInComingInvitePending,", isInComingInvitePending);
			Log.d("isIpv6Enabled,", isIpv6Enabled);
			Log.d("isMediaEncryptionMandatory,", isMediaEncryptionMandatory);
			Log.d("isKeepAliveEnabled,", isKeepAliveEnabled);
			Log.d("isMicMuted,", isMicMuted);
			Log.d("isNetworkReachable,", isNetworkReachable);
			Log.d("isSdp200AckEnabled,", isSdp200AckEnabled);
			Log.d("isSpeakerEnabled,", isSpeakerEnabled);
			Log.d("isVideoSupported,", isVideoSupported);
			Log.d("needsEchoCalibration,", needsEchoCalibration);
			Log.d("soundResourcesLocked,", soundResourcesLocked);
			Log.d("upnpAvailable,", upnpAvailable);

			Log.d("getAdaptiveRateAlgorithm,", getAdaptiveRateAlgorithm);
			Log.d("getAudioCodecs,", getAudioCodecs);
			Log.d("getAudioDscp,", getAudioDscp);
			Log.d("getAudioMulticastAddr,", getAudioMulticastAddr);
			Log.d("getAudioMulticastTtl,", getAudioMulticastTtl);
			Log.d("getAuthInfosList,", getAuthInfosList);
			Log.d("getCallLogs,", getCallLogs);
			Log.d("getCalls,", getCalls);
			Log.d("getCallsNb,", getCallsNb);
			//Log.d("getChatRoom,",getChatRoom);
			Log.d("getConference,", getConference);
			Log.d("getConferenceSize,", getConferenceSize);
			Log.d("getConfig,", getConfig);
			Log.d("getCurrentCall,", getCurrentCall);
			Log.d("getDefaultProxyConfig,", getDefaultProxyConfig);
			Log.d("getDownloadBandwidth,", getDownloadBandwidth);
			Log.d("getFileTransferServer,", getFileTransferServer);
			Log.d("getFirewallPolicy,", getFirewallPolicy);
			Log.d("getFriendList,", getFriendList);
			Log.d("getGlobalState,", getGlobalState);
			Log.d("getHttpProxyHost,", getHttpProxyHost);
			Log.d("getHttpProxyPort,", getHttpProxyPort);
			Log.d("getLastOutgoingCallLog,", getLastOutgoingCallLog);
			Log.d("getMaxCalls,", getMaxCalls);
			Log.d("getMediaEncryption,", getMediaEncryption);
			Log.d("getMissedCallsCount,", getMissedCallsCount);
			Log.d("getMtu,", getMtu);
			Log.d("getNortpTimeout,", getNortpTimeout);
			//Log.d("getOrCreateChatRoom,",getOrCreateChatRoom);
			//Log.d("getPayloadTypeBitrate,",getPayloadTypeBitrate);
			//Log.d("getPayloadTypeNumber,",getPayloadTypeNumber);
			Log.d("getPlaybackGain,", getPlaybackGain);
			Log.d("getPlayLevel,", getPlayLevel);
			Log.d("getPreferredFramerate,", getPreferredFramerate);
			Log.d("getPreferredVideoSize,", getPreferredVideoSize);
			Log.d("getPresenceModel,", getPresenceModel);
			Log.d("getPrimaryContact,", getPrimaryContact);
			Log.d("getPrimaryContactDisplayName,", getPrimaryContactDisplayName);
			Log.d("getProvisioningUri,", getProvisioningUri);
			Log.d("getProxyConfigList,", getProxyConfigList);
			Log.d("getRemoteAddress,", getRemoteAddress);
			Log.d("getRemoteRingbackTone,", getRemoteRingbackTone);
			Log.d("getRing,", getRing);
			Log.d("getSignalingTransportPorts,", getSignalingTransportPorts);
			Log.d("getSipDscp,", getSipDscp);
			Log.d("getSipTransportTimeout,", getSipTransportTimeout);
			Log.d("getStunServer,", getStunServer);
			Log.d("getSupportedVideoSizes,", getSupportedVideoSizes);
			Log.d("getUploadBandwidth,", getUploadBandwidth);
			Log.d("getUpnpExternalIpaddress,", getUpnpExternalIpaddress);
			Log.d("getUpnpState,", getUpnpState);
			Log.d("getUseRfc2833ForDtmfs,", getUseRfc2833ForDtmfs);
			Log.d("getUseSipInfoForDtmfs,", getUseSipInfoForDtmfs);
			Log.d("getVersion,", getVersion);
			Log.d("getVideoAutoAcceptPolicy,", getVideoAutoAcceptPolicy);
			Log.d("getVideoAutoInitiatePolicy,", getVideoAutoInitiatePolicy);
			Log.d("getVideoCodecs,", getVideoCodecs);
			Log.d("getVideoDevice,", getVideoDevice);
			Log.d("getVideoDscp,", getVideoDscp);
			Log.d("getVideoMulticastAddr,", getVideoMulticastAddr);
			Log.d("getVideoMulticastTtl,", getVideoMulticastTtl);
			Log.d("getVideoPreset,", getVideoPreset);
			Log.d("getPresenceInfo,", getPresenceInfo);
			Log.d("isTunnelAvailable,", isTunnelAvailable);
			Log.d("tunnelGetMode,", tunnelGetMode);
			Log.d("tunnelGetServers,", tunnelGetServers);
			Log.d("tunnelSipEnabled,", tunnelSipEnabled);
			Log.d("HWAccelDecode,", HWAcellDecode);
			Log.d("HWAccelEncode,", HWAcellEncode);
			Log.d("CameraParameters,", CameraParameters);
			new Thread() {
				public void run() {
					try {
						//print to csv
						String sdCard = Environment.getExternalStorageDirectory().toString() + "/ACE";

                        /* checks the file and if it already exist delete */
						final String fname = filename + ".csv";
						File file = new File(sdCard, fname);
						if (!file.exists()) {
							file.getParentFile().mkdirs();
						} else {
							file.delete();
						}

						Log.d("fname", fname);
						Log.d("file", file.getAbsolutePath());

						FileWriter fw = new FileWriter(file.getAbsoluteFile());

						fw.append("LinphoneCoreStatsTitle," + LinphoneCoreStatsTitle + "\n");
						fw.append("audioMulticastEnabled," + audioMulticastEnabled + "\n");
						fw.append("videoMulticastEnabled," + videoMulticastEnabled + "\n");
						fw.append("chatEnabled," + chatEnabled + "\n");
						fw.append("dnsSrvEnabled," + dnsSrvEnabled + "\n");
						fw.append("hasBuiltInEchoCanceler," + hasBuiltInEchoCanceler + "\n");
						fw.append("isAdaptiveRateControlEnabled," + isAdaptiveRateControlEnabled + "\n");
						fw.append("isEchoCancellationEnabled," + isEchoCancellationEnabled + "\n");
						fw.append("isEchoLimiterEnabled," + isEchoLimiterEnabled + "\n");
						fw.append("isIncall," + isIncall + "\n");
						fw.append("isInComingInvitePending," + isInComingInvitePending + "\n");
						fw.append("isIpv6Enabled," + isIpv6Enabled + "\n");
						fw.append("isMediaEncryptionMandatory," + isMediaEncryptionMandatory + "\n");
						fw.append("isKeepAliveEnabled," + isKeepAliveEnabled + "\n");
						fw.append("isMicMuted," + isMicMuted + "\n");
						fw.append("isNetworkReachable," + isNetworkReachable + "\n");
						fw.append("isSdp200AckEnabled," + isSdp200AckEnabled + "\n");
						fw.append("isSpeakerEnabled," + isSpeakerEnabled + "\n");
						fw.append("isVideoSupported," + isVideoSupported + "\n");
						fw.append("needsEchoCalibration," + needsEchoCalibration + "\n");
						fw.append("soundResourcesLocked," + soundResourcesLocked + "\n");
						fw.append("upnpAvailable," + upnpAvailable + "\n");

						fw.append("getAdaptiveRateAlgorithm," + getAdaptiveRateAlgorithm + "\n");
						fw.append("getAudioCodecs," + getAudioCodecs + "\n");
						fw.append("getAudioDscp," + getAudioDscp + "\n");
						fw.append("getAudioMulticastAddr," + getAudioMulticastAddr + "\n");
						fw.append("getAudioMulticastTtl," + getAudioMulticastTtl + "\n");
						fw.append("getAuthInfosList," + getAuthInfosList + "\n");
						fw.append("getCallLogs," + getCallLogs + "\n");
						fw.append("getCalls," + getCalls + "\n");
						fw.append("getCallsNb," + getCallsNb + "\n");
						//fw.append("getChatRoom,"+getChatRoom+"\n");
						fw.append("getConference," + getConference + "\n");
						fw.append("getConferenceSize," + getConferenceSize + "\n");
						fw.append("getConfig," + getConfig + "\n");
						fw.append("getCurrentCall," + getCurrentCall + "\n");
						fw.append("getDefaultProxyConfig," + getDefaultProxyConfig + "\n");
						fw.append("getDownloadBandwidth," + getDownloadBandwidth + "\n");
						fw.append("getFileTransferServer," + getFileTransferServer + "\n");
						fw.append("getFirewallPolicy," + getFirewallPolicy + "\n");
						fw.append("getFriendList," + getFriendList + "\n");
						fw.append("getGlobalState," + getGlobalState + "\n");
						fw.append("getHttpProxyHost," + getHttpProxyHost + "\n");
						fw.append("getHttpProxyPort," + getHttpProxyPort + "\n");
						fw.append("getLastOutgoingCallLog," + getLastOutgoingCallLog + "\n");
						fw.append("getMaxCalls," + getMaxCalls + "\n");
						fw.append("getMediaEncryption," + getMediaEncryption + "\n");
						fw.append("getMissedCallsCount," + getMissedCallsCount + "\n");
						fw.append("getMtu," + getMtu + "\n");
						fw.append("getNortpTimeout," + getNortpTimeout + "\n");
						//fw.append("getOrCreateChatRoom,"+getOrCreateChatRoom+"\n");
						//fw.append("getPayloadTypeBitrate,"+getPayloadTypeBitrate+"\n");
						//fw.append("getPayloadTypeNumber,"+getPayloadTypeNumber+"\n");
						fw.append("getPlaybackGain," + getPlaybackGain + "\n");
						fw.append("getPlayLevel," + getPlayLevel + "\n");
						fw.append("getPreferredFramerate," + getPreferredFramerate + "\n");
						fw.append("getPreferredVideoSize," + getPreferredVideoSize + "\n");
						fw.append("getPresenceModel," + getPresenceModel + "\n");
						fw.append("getPrimaryContact," + getPrimaryContact + "\n");
						fw.append("getPrimaryContactDisplayName," + getPrimaryContactDisplayName + "\n");
						fw.append("getProvisioningUri," + getProvisioningUri + "\n");
						fw.append("getProxyConfigList," + getProxyConfigList + "\n");
						fw.append("getRemoteAddress," + getRemoteAddress + "\n");
						fw.append("getRemoteRingbackTone," + getRemoteRingbackTone + "\n");
						fw.append("getRing," + getRing + "\n");
						fw.append("getSignalingTransportPorts," + getSignalingTransportPorts + "\n");
						fw.append("getSipDscp," + getSipDscp + "\n");
						fw.append("getSipTransportTimeout," + getSipTransportTimeout + "\n");
						fw.append("getStunServer," + getStunServer + "\n");
						fw.append("getSupportedVideoSizes," + getSupportedVideoSizes + "\n");
						fw.append("getUploadBandwidth," + getUploadBandwidth + "\n");
						fw.append("getUpnpExternalIpaddress," + getUpnpExternalIpaddress + "\n");
						fw.append("getUpnpState," + getUpnpState + "\n");
						fw.append("getUseRfc2833ForDtmfs," + getUseRfc2833ForDtmfs + "\n");
						fw.append("getUseSipInfoForDtmfs," + getUseSipInfoForDtmfs + "\n");
						fw.append("getVersion," + getVersion + "\n");
						fw.append("getVideoAutoAcceptPolicy," + getVideoAutoAcceptPolicy + "\n");
						fw.append("getVideoAutoInitiatePolicy," + getVideoAutoInitiatePolicy + "\n");
						fw.append("getVideoCodecs," + getVideoCodecs + "\n");
						fw.append("getVideoDevice," + getVideoDevice + "\n");
						fw.append("getVideoDscp," + getVideoDscp + "\n");
						fw.append("getVideoMulticastAddr," + getVideoMulticastAddr + "\n");
						fw.append("getVideoMulticastTtl," + getVideoMulticastTtl + "\n");
						fw.append("getVideoPreset," + getVideoPreset + "\n");
						fw.append("getPresenceInfo," + getPresenceInfo + "\n");
						fw.append("isTunnelAvailable," + isTunnelAvailable + "\n");
						fw.append("tunnelGetMode," + tunnelGetMode + "\n");
						fw.append("tunnelGetServers," + tunnelGetServers + "\n");
						fw.append("tunnelSipEnabled," + tunnelSipEnabled + "\n");
						fw.append("HWAccelDecode," + HWAcellDecode + "\n");
						fw.append("HWAccelEncode,"+HWAcellEncode+"\n");
						fw.append("CameraParameters," + CameraParameters + "\n");
						fw.close();


						//Copy Linphonerc into ace folder
						String path = LinphoneActivity.instance().getFilesDir().getAbsolutePath() + "/.linphonerc";

                        /* checks the file and if it already exist delete */
						final String fname1 = filename + "_linphonerc.txt";
						File file1 = new File(sdCard, fname1);
						if (!file1.exists()) {
							file1.getParentFile().mkdirs();
						} else {
							file1.delete();
						}

						Log.d("fname", fname1);
						Log.d("file", file1.getAbsolutePath());

						fw = new FileWriter(file1.getAbsoluteFile());
						fw.append(readFromFile(path));
						fw.close();

					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}.start();


			//show on screen logs
			//Display to developer

			stats_list.add("LinphoneCoreStatsTitle," + LinphoneCoreStatsTitle);
			stats_list.add("audioMulticastEnabled," + audioMulticastEnabled);
			stats_list.add("videoMulticastEnabled," + videoMulticastEnabled);
			stats_list.add("chatEnabled," + chatEnabled);
			stats_list.add("dnsSrvEnabled," + dnsSrvEnabled);
			stats_list.add("hasBuiltInEchoCanceler," + hasBuiltInEchoCanceler);
			stats_list.add("isAdaptiveRateControlEnabled," + isAdaptiveRateControlEnabled);
			stats_list.add("isEchoCancellationEnabled," + isEchoCancellationEnabled);
			stats_list.add("isEchoLimiterEnabled," + isEchoLimiterEnabled);
			stats_list.add("isIncall," + isIncall);
			stats_list.add("isInComingInvitePending," + isInComingInvitePending);
			stats_list.add("isIpv6Enabled," + isIpv6Enabled);
			stats_list.add("isMediaEncryptionMandatory," + isMediaEncryptionMandatory);
			stats_list.add("isKeepAliveEnabled," + isKeepAliveEnabled);
			stats_list.add("isMicMuted," + isMicMuted);
			stats_list.add("isNetworkReachable," + isNetworkReachable);
			stats_list.add("isSdp200AckEnabled," + isSdp200AckEnabled);
			stats_list.add("isSpeakerEnabled," + isSpeakerEnabled);
			stats_list.add("isVideoSupported," + isVideoSupported);
			stats_list.add("needsEchoCalibration," + needsEchoCalibration);
			stats_list.add("soundResourcesLocked," + soundResourcesLocked);
			stats_list.add("upnpAvailable," + upnpAvailable);

			stats_list.add("getAdaptiveRateAlgorithm," + getAdaptiveRateAlgorithm);
			stats_list.add("getAudioCodecs," + getAudioCodecs);
			stats_list.add("getAudioDscp," + getAudioDscp);
			stats_list.add("getAudioMulticastAddr," + getAudioMulticastAddr);
			stats_list.add("getAudioMulticastTtl," + getAudioMulticastTtl);
			stats_list.add("getAuthInfosList," + getAuthInfosList);
			stats_list.add("getCallLogs," + getCallLogs);
			stats_list.add("getCalls," + getCalls);
			stats_list.add("getCallsNb," + getCallsNb);
			//stats_list.add("getChatRoom,"+getChatRoom);
			stats_list.add("getConference," + getConference);
			stats_list.add("getConferenceSize," + getConferenceSize);
			stats_list.add("getConfig," + getConfig);
			stats_list.add("getCurrentCall," + getCurrentCall);
			stats_list.add("getDefaultProxyConfig," + getDefaultProxyConfig);
			stats_list.add("getDownloadBandwidth," + getDownloadBandwidth);
			stats_list.add("getFileTransferServer," + getFileTransferServer);
			stats_list.add("getFirewallPolicy," + getFirewallPolicy);
			stats_list.add("getFriendList," + getFriendList);
			stats_list.add("getGlobalState," + getGlobalState);
			stats_list.add("getHttpProxyHost," + getHttpProxyHost);
			stats_list.add("getHttpProxyPort," + getHttpProxyPort);
			stats_list.add("getLastOutgoingCallLog," + getLastOutgoingCallLog);
			stats_list.add("getMaxCalls," + getMaxCalls);
			stats_list.add("getMediaEncryption," + getMediaEncryption);
			stats_list.add("getMissedCallsCount," + getMissedCallsCount);
			stats_list.add("getMtu," + getMtu);
			stats_list.add("getNortpTimeout," + getNortpTimeout);
			//stats_list.add("getOrCreateChatRoom,"+getOrCreateChatRoom);
			//stats_list.add("getPayloadTypeBitrate,"+getPayloadTypeBitrate);
			//stats_list.add("getPayloadTypeNumber,"+getPayloadTypeNumber);
			stats_list.add("getPlaybackGain," + getPlaybackGain);
			stats_list.add("getPlayLevel," + getPlayLevel);
			stats_list.add("getPreferredFramerate," + getPreferredFramerate);
			stats_list.add("getPreferredVideoSize," + getPreferredVideoSize);
			stats_list.add("getPresenceModel," + getPresenceModel);
			stats_list.add("getPrimaryContact," + getPrimaryContact);
			stats_list.add("getPrimaryContactDisplayName," + getPrimaryContactDisplayName);
			stats_list.add("getProvisioningUri," + getProvisioningUri);
			stats_list.add("getProxyConfigList," + getProxyConfigList);
			stats_list.add("getRemoteAddress," + getRemoteAddress);
			stats_list.add("getRemoteRingbackTone," + getRemoteRingbackTone);
			stats_list.add("getRing," + getRing);
			stats_list.add("getSignalingTransportPorts," + getSignalingTransportPorts);
			stats_list.add("getSipDscp," + getSipDscp);
			stats_list.add("getSipTransportTimeout," + getSipTransportTimeout);
			stats_list.add("getStunServer," + getStunServer);
			stats_list.add("getSupportedVideoSizes," + getSupportedVideoSizes);
			stats_list.add("getUploadBandwidth," + getUploadBandwidth);
			stats_list.add("getUpnpExternalIpaddress," + getUpnpExternalIpaddress);
			stats_list.add("getUpnpState," + getUpnpState);
			stats_list.add("getUseRfc2833ForDtmfs," + getUseRfc2833ForDtmfs);
			stats_list.add("getUseSipInfoForDtmfs," + getUseSipInfoForDtmfs);
			stats_list.add("getVersion," + getVersion);
			stats_list.add("getVideoAutoAcceptPolicy," + getVideoAutoAcceptPolicy);
			stats_list.add("getVideoAutoInitiatePolicy," + getVideoAutoInitiatePolicy);
			stats_list.add("getVideoCodecs," + getVideoCodecs);
			stats_list.add("getVideoDevice," + getVideoDevice);
			stats_list.add("getVideoDscp," + getVideoDscp);
			stats_list.add("getVideoMulticastAddr," + getVideoMulticastAddr);
			stats_list.add("getVideoMulticastTtl," + getVideoMulticastTtl);
			stats_list.add("getVideoPreset," + getVideoPreset);
			stats_list.add("getPresenceInfo," + getPresenceInfo);
			stats_list.add("isTunnelAvailable," + isTunnelAvailable);
			stats_list.add("tunnelGetMode," + tunnelGetMode);
			stats_list.add("tunnelGetServers," + tunnelGetServers);
			stats_list.add("tunnelSipEnabled," + tunnelSipEnabled);
			stats_list.add("HWAccelDecode,"+ HWAcellDecode);
			stats_list.add("HWAccelEncode," + HWAcellEncode);

			stats_list.add("CameraParameters," + CameraParameters);
		}catch(Throwable e){
			e.printStackTrace();
		}
		return stats_list;
	}
	public String LC_Object_to_String(Object object){
		String string;
		try {

//			if(object instanceof PayloadType[]){
//				PayloadType[] payloadTypes=(PayloadType[])object;
//				string="";
//				for(int i=0; i<payloadTypes.length; i++){
//					string=string+payloadTypes[i].toString()+",";
//				}
//			}else if(object instanceof String[]) {
//				String[] String=(String[])object;
//				string="";
//				for(int i=0; i<String.length; i++){
//					string=string+String[i].toString()+",";
//				}
			string="";
			if(object == null)
				return "";
			if(object.getClass().isArray()) {//Handle Arrays


				for(int i=0; i< Array.getLength(object); i++) {
					if (Array.get(object, i) instanceof LinphoneAuthInfo) {
						LinphoneAuthInfo lai = (LinphoneAuthInfo) Array.get(object, i);
						string = string + "\n ,getUsername(): " + lai.getUsername() +
								"\n ,getUserId(): " + lai.getUserId() +
								"\n ,getPassword(): " + lai.getPassword() +
								"\n ,getDomain(): " + lai.getDomain() +
								"\n ,getHa1(): " + lai.getHa1() +
								"\n ,getRealm(): " + lai.getRealm();
					}else if(Array.get(object, i) instanceof PayloadType){
						string = string + "\n ,"+Array.get(object, i).toString()+",";
						string = string+"\n ,isPayloadTypeEnabled(),"+LC_Object_to_String(getLc().isPayloadTypeEnabled((PayloadType) Array.get(object, i)));
						string = string+"\n ,payloadTypeIsVbr(),"+LC_Object_to_String(getLc().payloadTypeIsVbr((PayloadType) Array.get(object, i)));


					}else if(Array.get(object, i) instanceof LinphoneProxyConfig) {
						LinphoneProxyConfig linphoneproxyconfig = (LinphoneProxyConfig) Array.get(object, i);
						string=LC_Object_to_String(linphoneproxyconfig);
					}else if(Array.get(object, i) instanceof LinphoneAddress) {
						LinphoneAddress linphoneaddress = (LinphoneAddress) Array.get(object, i);
						string=LC_Object_to_String(linphoneaddress);
					}else if(Array.get(object, i) instanceof LinphoneCallLog) {
						LinphoneCallLog linphonecalllog = (LinphoneCallLog) Array.get(object, i);
						string=LC_Object_to_String(linphonecalllog);
					}else {
						try {
							string = string + Array.get(object, i).toString()+",";
						} catch (Throwable e) {
							string = object.toString();
						}
					}
				}

			}else {//Handle Objects

				if (object instanceof LpConfig) {
					LpConfig lpconfig = (LpConfig) object;

					String path = LinphoneActivity.instance().getFilesDir().getAbsolutePath() + "/.linphonerc";
					//string=path+"\n"+readFromFile(path);
					string = path;
				}else if(object instanceof LinphoneAddress) {
					LinphoneAddress linephoneaddress = (LinphoneAddress) object;
					string = string + "\n ,linephoneaddress.asString():" + LC_Object_to_String(linephoneaddress.asString()) +
							"\n ,linephoneaddress.asStringUriOnly(): " + LC_Object_to_String(linephoneaddress.asStringUriOnly())+
							"\n ,linephoneaddress.getDisplayName(): " + LC_Object_to_String(linephoneaddress.getDisplayName())+
							"\n ,linephoneaddress.getDomain(): " + LC_Object_to_String(linephoneaddress.getDomain())+
							"\n ,linephoneaddress.getPort(): " + LC_Object_to_String(linephoneaddress.getPort())+
							"\n ,linephoneaddress.getTransport(): " + LC_Object_to_String(linephoneaddress.getTransport())+
							"\n ,linephoneaddress.getUserName(): " + LC_Object_to_String(linephoneaddress.getUserName());


				}else if(object instanceof LinphoneCallLog) {

					try {
						LinphoneCallLog linephonecalllog = (LinphoneCallLog) object;
						string = string + "\n ,linephonecalllog.getCallDuration():" + formatHHMMSS(linephonecalllog.getCallDuration()) +
								"\n ,linephonecalllog.getCallId(): " + LC_Object_to_String(linephonecalllog.getCallId()) +
								"\n ,linephonecalllog.getDirection(): " + LC_Object_to_String(linephonecalllog.getDirection()) +
								"\n ,linephonecalllog.getFrom(): " + LC_Object_to_String(linephonecalllog.getFrom()) +
								"\n ,linephonecalllog.getStartDate(): " + LC_Object_to_String(linephonecalllog.getStartDate()) +
								"\n ,linephonecalllog.getStatus(): " + LC_Object_to_String(linephonecalllog.getStatus()) +
								"\n ,linephonecalllog.getTimestamp(): " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z").format(new Date(linephonecalllog.getTimestamp())) +
								"\n ,linephonecalllog.getTo(): " + LC_Object_to_String(linephonecalllog.getTo());
					}catch(Throwable e){
						string="CORRUPT";
					}


				}else if(object instanceof LinphoneProxyConfig) {
					LinphoneProxyConfig linphoneproxyconfig = (LinphoneProxyConfig) object;
					string = string + "\n ,linphoneproxyconfig.avpfEnabled():" + LC_Object_to_String(linphoneproxyconfig.avpfEnabled()) +
							"\n ,linphoneproxyconfig.getAddress(): " + LC_Object_to_String(linphoneproxyconfig.getAddress())+
							"\n ,linphoneproxyconfig.getAvpfRRInterval(): " + LC_Object_to_String(linphoneproxyconfig.getAvpfRRInterval())+
							"\n ,linphoneproxyconfig.getContactParameters(): " + LC_Object_to_String(linphoneproxyconfig.getContactParameters())+
							"\n ,linphoneproxyconfig.getContactUriParameters(): " + LC_Object_to_String(linphoneproxyconfig.getContactUriParameters())+
							"\n ,linphoneproxyconfig.getDialPrefix(): " + LC_Object_to_String(linphoneproxyconfig.getDialPrefix())+
							"\n ,linphoneproxyconfig.getDomain(): " + LC_Object_to_String(linphoneproxyconfig.getDomain())+
							"\n ,linphoneproxyconfig.getError(): " + LC_Object_to_String(linphoneproxyconfig.getError())+
							"\n ,linphoneproxyconfig.getErrorInfo(): " + LC_Object_to_String(linphoneproxyconfig.getErrorInfo())+
							"\n ,linphoneproxyconfig.getIdentity(): " + LC_Object_to_String(linphoneproxyconfig.getIdentity())+
							"\n ,linphoneproxyconfig.getPrivacy(): " + LC_Object_to_String(linphoneproxyconfig.getPrivacy())+
							"\n ,linphoneproxyconfig.getProxy(): " + LC_Object_to_String(linphoneproxyconfig.getProxy())+
							"\n ,linphoneproxyconfig.getPublishExpires(): " + LC_Object_to_String(linphoneproxyconfig.getPublishExpires())+
							"\n ,linphoneproxyconfig.getQualityReportingCollector(): " + LC_Object_to_String(linphoneproxyconfig.getQualityReportingCollector())+
							"\n ,linphoneproxyconfig.getQualityReportingInterval(): " + LC_Object_to_String(linphoneproxyconfig.getQualityReportingInterval())+
							"\n ,linphoneproxyconfig.getRealm(): " + LC_Object_to_String(linphoneproxyconfig.getRealm())+
							"\n ,linphoneproxyconfig.getRoute(): " + LC_Object_to_String(linphoneproxyconfig.getRoute())+
							"\n ,linphoneproxyconfig.getState(): " + LC_Object_to_String(linphoneproxyconfig.getState())+
							"\n ,linphoneproxyconfig.getUserData(): " + LC_Object_to_String(linphoneproxyconfig.getUserData())+
							//"\n ,linphoneproxyconfig.isPhoneNumber(): " + LC_Object_to_String(linphoneproxyconfig.isPhoneNumber())+
							"\n ,linphoneproxyconfig.isRegistered(): " + LC_Object_to_String(linphoneproxyconfig.isRegistered())+
							"\n ,linphoneproxyconfig.publishEnabled(): " + LC_Object_to_String(linphoneproxyconfig.publishEnabled())+
							"\n ,linphoneproxyconfig.qualityReportingEnabled(): " + LC_Object_to_String(linphoneproxyconfig.qualityReportingEnabled())+
							"\n ,linphoneproxyconfig.registerEnabled(): " + LC_Object_to_String(linphoneproxyconfig.registerEnabled());



				}else if(object instanceof LinphoneCore.MediaEncryption){
					try {
						string = object != null ? object.toString() : "null";
					} catch (Throwable e) {
						string = object != null ? String.valueOf(object) : "null";
					}
					string = string+"\nmediaEncryptionSupported,"+LC_Object_to_String(getLc().mediaEncryptionSupported((LinphoneCore.MediaEncryption)object));
				}else {
					try {
						string = object != null ? object.toString() : "null";
					} catch (Throwable e) {
						string = object != null ? String.valueOf(object) : "null";
					}
				}

			}
		}catch(Throwable e){
			string = "null";
			e.printStackTrace();
		}
		return string;
	}

	public static void display_original_JSON(final JSONObject jsonobject, final String filename){
		try {


			new Thread() {
				public void run() {
					try {
						//print to csv
						String sdCard = Environment.getExternalStorageDirectory().toString() + "/ACE";

                        /* checks the file and if it already exist delete */
						final String fname = filename + ".csv";
						File file = new File(sdCard, fname);
						if (!file.exists()) {
							file.getParentFile().mkdirs();
						} else {
							file.delete();
						}

						Log.d("fname", fname);
						Log.d("file", file.getAbsolutePath());

						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						fw.append("Original JSON," + "\n" + jsonobject.toString(4));

						fw.close();


					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}.start();

			//Display to developer
			Log.d("Original JSON," + "\n"+ jsonobject.toString(4));


		}catch(Throwable e){
			e.printStackTrace();
		}

	}
	public static void display_incorporated_autoconfig_values(final JsonConfig config, final String filename){
		try {


			new Thread() {
				public void run() {
					try {
						//print to csv
						String sdCard = Environment.getExternalStorageDirectory().toString() + "/ACE";

                        /* checks the file and if it already exist delete */
						final String fname = filename + ".csv";
						File file = new File(sdCard, fname);
						if (!file.exists()) {
							file.getParentFile().mkdirs();
						} else {
							file.delete();
						}

						Log.d("fname", fname);
						Log.d("file", file.getAbsolutePath());

						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						fw.append("Auto-Config Values"+"\n");
						fw.append("_request_url"+config._request_url+"\n");
						fw.append("_version"+config._version+"\n");
						fw.append("_expiration_time"+config._expiration_time+"\n");
						fw.append("_configuration_auth_password;"+config._configuration_auth_password+"\n");
						fw.append("_configuration_auth_expiration"+config._configuration_auth_expiration+"\n");
						fw.append("_sip_registration_maximum_threshold"+config._sip_registration_maximum_threshold+"\n");
						fw.append("_sip_register_usernames"+config._sip_register_usernames+"\n");
						fw.append("_sip_auth_username"+config._sip_auth_username+"\n");
						fw.append("_sip_auth_password"+config._sip_auth_password+"\n");
						fw.append("_sip_register_domain"+config._sip_register_domain+"\n");
						fw.append("_sip_register_port"+config._sip_register_port+"\n");
						fw.append("_sip_register_transport"+config._sip_register_transport+"\n");
						fw.append("_enable_echo_cancellation"+config._enable_echo_cancellation+"\n");
						fw.append("_enable_video"+config._enable_video+"\n");
						fw.append("_enable_rtt"+config._enable_rtt+"\n");
						fw.append("_enable_adaptive_rate"+config._enable_adaptive_rate+"\n");
						fw.append("_enabled_codecs"+config._enabled_codecs+"\n");
						fw.append("_bwLimit"+config._bwLimit+"\n");
						fw.append("_upload_bandwidth"+config._upload_bandwidth+"\n");
						fw.append("_download_bandwidth"+config._download_bandwidth+"\n");
						fw.append("_enable_stun"+config._enable_stun+"\n");
						fw.append("_stun_server"+config._stun_server+"\n");
						fw.append("_enable_ice"+config._enable_ice+"\n");
						fw.append("_logging"+config._logging+"\n");
						fw.append("_sip_mwi_uri"+config._sip_mwi_uri+"\n");
						fw.append("_sip_videomail_uri"+config._sip_videomail_uri+"\n");
						fw.append("_video_resolution_maximum"+config._video_resolution_maximum+"\n");
						fw.append("_video_preferred_frames_per_second"+config._video_preferred_frames_per_second+"\n");
						fw.close();


					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}.start();

			//Display to developer
			Log.d("Auto-Config Values"+"\n");
			Log.d("_request_url"+config._request_url+"\n");
			Log.d("_version"+config._version+"\n");
			Log.d("_expiration_time"+config._expiration_time+"\n");
			Log.d("_configuration_auth_password;"+config._configuration_auth_password+"\n");
			Log.d("_configuration_auth_expiration"+config._configuration_auth_expiration+"\n");
			Log.d("_sip_registration_maximum_threshold"+config._sip_registration_maximum_threshold+"\n");
			Log.d("_sip_register_usernames"+config._sip_register_usernames+"\n");
			Log.d("_sip_auth_username"+config._sip_auth_username+"\n");
			Log.d("_sip_auth_password"+config._sip_auth_password+"\n");
			Log.d("_sip_register_domain"+config._sip_register_domain+"\n");
			Log.d("_sip_register_port"+config._sip_register_port+"\n");
			Log.d("_sip_register_transport"+config._sip_register_transport+"\n");
			Log.d("_enable_echo_cancellation"+config._enable_echo_cancellation+"\n");
			Log.d("_enable_video"+config._enable_video+"\n");
			Log.d("_enable_rtt"+config._enable_rtt+"\n");
			Log.d("_enable_adaptive_rate"+config._enable_adaptive_rate+"\n");
			Log.d("_enabled_codecs"+config._enabled_codecs+"\n");
			Log.d("_bwLimit"+config._bwLimit+"\n");
			Log.d("_upload_bandwidth"+config._upload_bandwidth+"\n");
			Log.d("_download_bandwidth"+config._download_bandwidth+"\n");
			Log.d("_enable_stun"+config._enable_stun+"\n");
			Log.d("_stun_server"+config._stun_server+"\n");
			Log.d("_enable_ice"+config._enable_ice+"\n");
			Log.d("_logging"+config._logging+"\n");
			Log.d("_sip_mwi_uri"+config._sip_mwi_uri+"\n");
			Log.d("_sip_videomail_uri"+config._sip_videomail_uri+"\n");
			Log.d("_video_resolution_maximum"+config._video_resolution_maximum+"\n");
			Log.d("_video_preferred_frames_per_second"+config._video_preferred_frames_per_second+"\n");


		}catch(Throwable e){
			e.printStackTrace();
		}

	}

	private String readFromFile(String filepath) {

		String ret = "";

		try {
			InputStream inputStream = openFileInput(".linphonerc");

			if ( inputStream != null ) {
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String receiveString = "";
				StringBuilder stringBuilder = new StringBuilder();

				while ( (receiveString = bufferedReader.readLine()) != null ) {
					stringBuilder.append(receiveString);
				}

				inputStream.close();
				ret = stringBuilder.toString();
			}
		}
		catch (FileNotFoundException e) {
			Log.e("login activity", "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e("login activity", "Can not read file: " + e.toString());
		}

		return ret;
	}
	public String formatHHMMSS(int secondsCount){
		//Calculate the seconds to display:
		int seconds = secondsCount %60;
		secondsCount -= seconds;
		//Calculate the minutes:
		long minutesCount = secondsCount / 60;
		long minutes = minutesCount % 60;
		minutesCount -= minutes;
		//Calculate the hours:
		long hoursCount = minutesCount / 60;
		//Build the String
		return "" + hoursCount + ":" + minutes + ":" + seconds;
	}
}

interface ContactPicked {
	void setAddresGoToDialerAndCall(String number, String name, Uri photo);
	void goToDialer();
}
