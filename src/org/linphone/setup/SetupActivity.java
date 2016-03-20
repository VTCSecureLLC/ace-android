package org.linphone.setup;
/*
SetupActivity.java
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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.LinphonePreferences;
import org.linphone.LinphonePreferences.AccountBuilder;
import org.linphone.LinphoneService;
import org.linphone.R;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAddress.TransportType;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.custom.LoginMainActivity;
import org.linphone.mediastream.Log;
import org.linphone.vtcsecure.Utils;

/**
 * @author Sylvain Berfini
 */
public class SetupActivity extends FragmentActivity implements OnClickListener {
	public final static String AUTO_CONFIG_SUCCED_EXTRA = "config_succeed_extra";
	private static SetupActivity instance;
	//private RelativeLayout back, next, cancel;
	private SetupFragmentsEnum currentFragment;
	private SetupFragmentsEnum firstFragment;
	private Fragment fragment;
	private LinphonePreferences mPrefs;
	private boolean accountCreated = false;
	private String username;
	private boolean isJsonConfigSucceed = false;
	private LinphoneCoreListenerBase mListener;
	private LinphoneAddress address;
	ProgressDialog mProgressDialog;
	//URL format for registrar lookup
	final String registrarSRVLookupFormat="_sip._tcp.%domain%";
	final String registrarSRVLookupFormatTLS="_sips._tcp.%domain%";
	//URL format for autoConfig lookup
	final String autoConfigSRVLookupFormat="_rueconfig._tls.%domain%";
	private Handler mHandler;
	private ServiceWaitThread mThread;

	static int WIFI_ACTIVITY_RESULT=0;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		if (getResources().getBoolean(R.bool.isTablet) && getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        }
		mHandler = new Handler();
		
		setContentView(R.layout.setup);
		CDNProviders.getInstance().setContext(getApplicationContext());
		firstFragment = getResources().getBoolean(R.bool.setup_use_linphone_as_first_fragment) ?
				SetupFragmentsEnum.LINPHONE_LOGIN : SetupFragmentsEnum.MENU;
		firstFragment = SetupFragmentsEnum.GENERIC_LOGIN;
        if (findViewById(R.id.fragmentContainer) != null) {
            if (savedInstanceState == null) {
            	display(firstFragment);
            } else {
            	currentFragment = (SetupFragmentsEnum) savedInstanceState.getSerializable("CurrentFragment");
            }
        }
        mPrefs = LinphonePreferences.instance();
        initUI();
        
        mListener = new LinphoneCoreListenerBase(){
        	@Override
        	public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, LinphoneCore.RegistrationState state, String smessage) {
				Log.d("test state: " + state);
				if(accountCreated){
					if(address != null && address.asString().equals(cfg.getIdentity()) ) {
						if (state == RegistrationState.RegistrationOk) {
							if (LinphoneManager.getLc().getDefaultProxyConfig() != null) {
								launchEchoCancellerCalibration(true);
							}

						} else if (state != RegistrationState.RegistrationProgress && state != RegistrationState.RegistrationCleared) {
							Toast.makeText(SetupActivity.this, getString(R.string.first_launch_bad_login_password), Toast.LENGTH_LONG).show();
							deleteAccounts();
						}
						else if (state.equals(RegistrationState.RegistrationCleared)) {
							if (lc != null) {
								LinphoneAuthInfo authInfo = lc.findAuthInfo(cfg.getIdentity(), cfg.getRealm(), cfg.getDomain());
								if (authInfo != null)
									lc.removeAuthInfo(authInfo);
							}
						}
					}
				}
        	}
        };
        
        instance = this;
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		Utils.check_network_status(this, WIFI_ACTIVITY_RESULT);//Anytime activity is resumed and we don't have internet, tell the user.. and offer them to turn on wifi.
//		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
//		if (lc != null) {
//			lc.addListener(mListener);
//		}

	}
	
	@Override
	protected void onPause() {

		if(accountCreated && !LinphoneService.isReady() && (LinphoneManager.getLc().getDefaultProxyConfig()==null || !LinphoneManager.getLc().getDefaultProxyConfig().isRegistered()) )
		{
			deleteAccounts();
		}
		
		super.onPause();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("CurrentFragment", currentFragment);
		super.onSaveInstanceState(outState);
	}
	
	public static SetupActivity instance() {
		return instance;
	}

	private void initUI() {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(R.string.please_wait);
		/*back = (RelativeLayout) findViewById(R.id.setup_back);
		back.setOnClickListener(this);
		next = (RelativeLayout) findViewById(R.id.setup_next);
		next.setOnClickListener(this);
		cancel = (RelativeLayout) findViewById(R.id.setup_cancel);
		cancel.setOnClickListener(this);*/
	}

	void deleteAccounts()
	{
		Log.d("test delete account ");
		AccountHelper.deleteAccount(SetupActivity.this);
		accountCreated = false;
		int nbAccounts = mPrefs.getAccountCount();
		for (int i = 0; i < nbAccounts; i++) {
			LinphonePreferences.instance().deleteAccount(i);
		}
	}
	
	private void changeFragment(Fragment newFragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		
//		transaction.addToBackStack("");
		transaction.replace(R.id.fragmentContainer, newFragment);
		
		transaction.commitAllowingStateLoss();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		switch (id) {
		case R.id.btn_login_1:
			displayLoginGeneric();
			break;
		case R.id.btn_login_2:
			displayLoginGeneric();
			break;
		case R.id.btn_login_3:
			displayLoginGeneric();
			break;
		default:
			break;
		}
		
		if (id == R.id.setup_cancel) {
			LinphonePreferences.instance().firstLaunchSuccessful();
			if (getResources().getBoolean(R.bool.setup_cancel_move_to_back)) {
				moveTaskToBack(true);
			} else {
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		} else if (id == R.id.setup_next) {
			if (firstFragment == SetupFragmentsEnum.LINPHONE_LOGIN) {
				LinphoneLoginFragment linphoneFragment = (LinphoneLoginFragment) fragment;
				linphoneFragment.linphoneLogIn();
			} else {
				
				if (currentFragment == SetupFragmentsEnum.WELCOME) {
					LoginMainActivity fragment = new LoginMainActivity();
					changeFragment(fragment);
					currentFragment = SetupFragmentsEnum.MENU;
					
					//next.setVisibility(View.GONE);
					//back.setVisibility(View.VISIBLE);
				} else if (currentFragment == SetupFragmentsEnum.WIZARD_CONFIRM) {
					finish();
				}
			}
		} else if (id == R.id.setup_back) {
			onBackPressed();
		}
	}

	@Override
	public void onBackPressed() {
		if(currentFragment == SetupFragmentsEnum.MENU || currentFragment == SetupFragmentsEnum.GENERIC_LOGIN){
			Intent i = new Intent();
			i.putExtra("Exit", true);
			setResult(RESULT_OK, i);
			super.onBackPressed();
			return;
		}
		if (currentFragment == firstFragment) {
			LinphonePreferences.instance().firstLaunchSuccessful();
			if (getResources().getBoolean(R.bool.setup_cancel_move_to_back)) {
				moveTaskToBack(true);
			} else {
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
		}
		if (currentFragment == SetupFragmentsEnum.MENU) {
			WelcomeFragment fragment = new WelcomeFragment();
			changeFragment(fragment);
			currentFragment = SetupFragmentsEnum.WELCOME;
			
			//next.setVisibility(View.VISIBLE);
			//back.setVisibility(View.GONE);
		} else if (currentFragment == SetupFragmentsEnum.GENERIC_LOGIN 
				|| currentFragment == SetupFragmentsEnum.LINPHONE_LOGIN 
				|| currentFragment == SetupFragmentsEnum.WIZARD 
				|| currentFragment == SetupFragmentsEnum.REMOTE_PROVISIONING) {
			LoginMainActivity fragment = new LoginMainActivity();
			changeFragment(fragment);
			currentFragment = SetupFragmentsEnum.MENU;
		} else if (currentFragment == SetupFragmentsEnum.WELCOME) {
			finish();
		}
	}

	private void launchEchoCancellerCalibration(boolean sendEcCalibrationResult) {
		boolean needsEchoCalibration = LinphoneManager.getLc().needsEchoCalibration();

		if (needsEchoCalibration && mPrefs.isFirstLaunch()) {
			mPrefs.setAccountEnabled(mPrefs.getAccountCount() - 1, false); //We'll enable it after the echo calibration
			EchoCancellerCalibrationFragment fragment = new EchoCancellerCalibrationFragment();
			fragment.enableEcCalibrationResultSending(sendEcCalibrationResult);
			changeFragment(fragment);
			currentFragment = SetupFragmentsEnum.ECHO_CANCELLER_CALIBRATION;
			/*back.setVisibility(View.VISIBLE);
			next.setVisibility(View.GONE);
			next.setEnabled(false);
			cancel.setEnabled(false);*/
		} else {
			success();
		}		
	}


	/**
	 * @param username SIP username
	 * @param password SIP password
	 * @param domain SIP domain
	 * @param userId SIP userId
	 * @param transport_type SIP Transport type
	 * @param port SIP port number
	 * @param sendEcCalibrationResult Send echo cancellation result
	 */
	private void logIn(final String username, final String password, final String domain, final String userId, final TransportType transport_type, final String port, final boolean sendEcCalibrationResult) {


		mProgressDialog.show();
		long time = System.currentTimeMillis();
		if(LinphoneManager.isInstanciated())
			LinphoneManager.destroy();
		AccountHelper.setAccount(this, username);
		LinphoneManager.createAndStart(this);


		long time2 = System.currentTimeMillis();

		Log.d("TOOOK:    "+ (time2- time));
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.addListener(mListener);
		}



		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && getCurrentFocus() != null) {
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}


		//Create rue config lookup url from domain and attempt to retrieve config
		String configURL = autoConfigSRVLookupFormat.replace("%domain%", domain.toString().replaceAll("\\s", ""));
		JsonConfig.refreshConfig(configURL, username, password, new JsonConfig.ConfigListener() {


			@Override
			public void onParsed(JsonConfig config) {
				isJsonConfigSucceed = true;
				String sip_username, sip_password;
				if (config.getSipAuthPassword() == null || config.getSipAuthUsername() == null || config.getSipAuthPassword().length() == 0 || config.getSipAuthUsername().length() == 0) {
					sip_username = username;
					sip_password = password;
				} else {
					sip_username = config.getSipAuthUsername();
					sip_password = config.getSipAuthPassword();
				}
				saveCreatedAccount(username, password, domain, userId, transport_type, port);
				config.applySettings(transport_type, port);

				mProgressDialog.dismiss();
			}

			@Override
			public void onFailed(String reason) {
				mProgressDialog.dismiss();
				isJsonConfigSucceed = false;
				try {
					saveCreatedAccount(username, password, domain, userId, transport_type, port);
				} catch (Exception e) {
					Toast.makeText(SetupActivity.this, "Failed to register", Toast.LENGTH_SHORT).show();
				}
			}
		});



	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(!LinphoneService.isReady() && LinphoneManager.isInstanciated())
		{
			LinphoneManager.destroy();
		}
	}

	private void logIn(final String username, final String password, final String domain, final TransportType transport_type, final String port, final boolean sendEcCalibrationResult) {


		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && getCurrentFocus() != null) {
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}
		//show progress bar

		mProgressDialog.show();
		JsonConfig.refreshConfig(domain, username, password, new JsonConfig.ConfigListener() {


			@Override
			public void onParsed(JsonConfig config) {
				String sip_username, sip_password;
				if (config.getSipAuthPassword() == null || config.getSipAuthUsername() == null || config.getSipAuthPassword().length() == 0 || config.getSipAuthUsername().length() == 0) {
					sip_username = username;
					sip_password = password;
				} else {
					sip_username = config.getSipAuthUsername();
					sip_password = config.getSipAuthPassword();
				}
				saveCreatedAccount(sip_username, sip_password, sip_username, domain, transport_type, port);
				config.applySettings(transport_type, port);

				mProgressDialog.dismiss();
			}

			@Override
			public void onFailed(String reason) {
				Toast.makeText(LinphoneManager.getInstance().getContext(), reason, Toast.LENGTH_LONG).show();
				mProgressDialog.dismiss();
			}
		});



	}
	
	public void checkAccount(String username, String password, String domain, String userId, TransportType transport_type, String port) {
		saveCreatedAccount(username, password, domain, userId, transport_type, port);
	}

	public void linphoneLogIn(String username, String password, String domain, String userId, TransportType transport_type, String port, boolean validate) {
		if (validate) {
			checkAccount(username, password, domain, userId, transport_type, port);
		} else {
			logIn(username, password, domain, userId, transport_type, port, true);
		}
	}

	public void genericLogIn(String username, String password, String domain, String userId, TransportType transport_type, String port) {
		logIn(username, password, domain, userId, transport_type, port, false);
	}

	private void display(SetupFragmentsEnum fragment) {
		switch (fragment) {
		case WELCOME:
			displayWelcome();
			break;
		case LINPHONE_LOGIN:
			displayLoginLinphone();
			break;
		case MENU : 
			displayMenu();
			break;
		case GENERIC_LOGIN:
			displayLoginGeneric();
			break;
		default:
			throw new IllegalStateException("Can't handle " + fragment);
		}
		//After displaying surface fragment
		Utils.check_network_status(this, WIFI_ACTIVITY_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==WIFI_ACTIVITY_RESULT){
			Intent refresh = new Intent(this, SetupActivity.class);
			startActivity(refresh);
			this.finish();
		}
	}

	public void displayMenu() {
		LoginMainActivity fragment = new LoginMainActivity();
		changeFragment(fragment);
		currentFragment = SetupFragmentsEnum.MENU;
		
	}

	public void displayWelcome() {
		fragment = new WelcomeFragment();
		changeFragment(fragment);
		currentFragment = SetupFragmentsEnum.WELCOME;
	}

	public void displayLoginGeneric() {
		
		
		fragment = new GenericLoginFragment();
		changeFragment(fragment);
		currentFragment = SetupFragmentsEnum.GENERIC_LOGIN;

	}
	
	public void displayLoginLinphone() {
		fragment = new LinphoneLoginFragment();
		changeFragment(fragment);
		currentFragment = SetupFragmentsEnum.LINPHONE_LOGIN;
	}

	public void displayWizard() {
		fragment = new WizardFragment();
		changeFragment(fragment);
		currentFragment = SetupFragmentsEnum.WIZARD;
	}

	public void displayRemoteProvisioning() {
		fragment = new RemoteProvisioningFragment();
		changeFragment(fragment);
		currentFragment = SetupFragmentsEnum.REMOTE_PROVISIONING;
	}
	
	public void saveCreatedAccount(String username, String password, String domain, String userId, TransportType transport_type, String port) {
		if (accountCreated)
			return;

		if(username.startsWith("sip:")) {
			username = username.substring(4);
		}

		if (username.contains("@"))
			username = username.split("@")[0];

		if(domain.startsWith("sip:")) {
			domain = domain.substring(4);
		}

		String identity = "sip:" + username + "@" + domain;
		Log.d("identity="+identity);
		try {
			address = LinphoneCoreFactory.instance().createLinphoneAddress(identity);
		} catch (LinphoneCoreException e) {
			e.printStackTrace();
		}

		boolean isMainAccountLinphoneDotOrg;
		if(domain.equals(getBaseContext().getString(R.string.default_domain))){
			isMainAccountLinphoneDotOrg=true;
		}else{
			isMainAccountLinphoneDotOrg=false;
		}

		boolean useLinphoneDotOrgCustomPorts;
		if(port.equals(getBaseContext().getString(R.string.default_port))){
			useLinphoneDotOrgCustomPorts=true;
		}else{
			useLinphoneDotOrgCustomPorts=false;
		}

		AccountBuilder builder = new AccountBuilder(LinphoneManager.getLc())
		.setUsername(username)
		.setDomain(domain)
		.setUserId(userId)
		.setPassword(password)
		.setExpires("280");
		Log.d("isMainAccountLinphoneDotOrg=" + isMainAccountLinphoneDotOrg);
		Log.d("useLinphoneDotOrgCustomPorts=" + useLinphoneDotOrgCustomPorts);
		if (isMainAccountLinphoneDotOrg && useLinphoneDotOrgCustomPorts) {
			Log.d("Setting default values");
			//if (getResources().getBoolean(R.bool.disable_all_security_features_for_markets)) {
			//	builder.setProxy(domain + ":5060")
			//	.setTransport(TransportType.LinphoneTransportTcp);
			//}
			//else {
			//	builder.setProxy(domain + ":5061")
			//	.setTransport(TransportType.LinphoneTransportTls);
			//}
			Log.d("builder.setProxy", domain + ":" + port);
			builder.setProxy(domain + ":" +port)
			.setTransport(transport_type)
			.setOutboundProxyEnabled(true)
			.setAvpfEnabled(true)
			.setAvpfRRInterval(3)
			.setQualityReportingCollector("sip:voip-metrics@bc1.vatrp.net")
			.setQualityReportingEnabled(true)
			.setQualityReportingInterval(180)
			.setRealm(domain);

			mPrefs.setMediaEncryption(LinphoneCore.MediaEncryption.None);
			mPrefs.setStunServer(getString(R.string.default_stun));
			mPrefs.setIceEnabled(true);
		} else {
//			String forcedProxy = getResources().getString(R.string.setup_forced_proxy);
//			if (!TextUtils.isEmpty(forcedProxy)) {
//				builder.setProxy(forcedProxy)
//				.setOutboundProxyEnabled(true)
//				.setAvpfRRInterval(5);
//			}

			Log.d("builder.setProxy", domain + ":" + port);
			builder.setProxy(domain + ":" + port)
					.setTransport(transport_type)
					.setOutboundProxyEnabled(true)
					.setAvpfEnabled(false)
							//.setAvpfRRInterval(3)
					//.setQualityReportingCollector("sip:voip-metrics@bc1.vatrp.net")
					//.setQualityReportingEnabled(true)
					//.setQualityReportingInterval(180)
					.setRealm(domain);


			//mPrefs.setStunServer(getString(R.string.default_stun));
			mPrefs.setMediaEncryption(LinphoneCore.MediaEncryption.None);
			mPrefs.setIceEnabled(false);
		}
		
		if (getResources().getBoolean(R.bool.enable_push_id)) {
			String regId = mPrefs.getPushNotificationRegistrationID();
			String appId = getString(R.string.push_sender_id);
			if (regId != null && mPrefs.isPushNotificationEnabled()) {
				String contactInfos = "app-id=" + appId + ";pn-type=google;pn-tok=" + regId;
				builder.setContactParameters(contactInfos);
			}
		}
		
		try {
			builder.saveNewAccount();
			accountCreated = true;
			this.username = username;
		} catch (LinphoneCoreException e) {
			e.printStackTrace();
		}


		Log.d("test accounts: " +  mPrefs.getAccountCount());

	}

	public void displayWizardConfirm(String username) {
		WizardConfirmFragment fragment = new WizardConfirmFragment();

		Bundle extras = new Bundle();
		extras.putString("Username", username);
		fragment.setArguments(extras);
		changeFragment(fragment);

		currentFragment = SetupFragmentsEnum.WIZARD_CONFIRM;

		/*next.setVisibility(View.VISIBLE);
		next.setEnabled(false);
		back.setVisibility(View.GONE);*/
	}

	public void isAccountVerified(String username) {
		Toast.makeText(this, getString(R.string.setup_account_validated), Toast.LENGTH_LONG).show();
		LinphoneManager.getLcIfManagerNotDestroyedOrNull().refreshRegisters();
		launchEchoCancellerCalibration(true);
	}

	public void isEchoCalibrationFinished() {
		mPrefs.setAccountEnabled(mPrefs.getAccountCount() - 1, true);
		success();
	}
	
	public void success() {
		mPrefs.firstLaunchSuccessful();


		mHandler.post(new Runnable() {
			@Override
			public void run() {

				AccountHelper.setAccount(SetupActivity.this, username);
				LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
				if (lc != null) {
					lc.removeListener(mListener);
				}
				LinphoneManager.destroy();
				startService(new Intent(Intent.ACTION_MAIN).setClass(SetupActivity.this, LinphoneService.class));
			}
		});

		mThread = new ServiceWaitThread();
		mThread.start();

	}

	void onServiceReady()

	{


		Intent i = new Intent(SetupActivity.this, LinphoneActivity.class);
		i.putExtra(AUTO_CONFIG_SUCCED_EXTRA, isJsonConfigSucceed);
		startActivity(i);
		finish();
	}




	private class ServiceWaitThread extends Thread {
		public void run() {


			while (!LinphoneService.isReady()) {
				try {
					sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException("waiting thread sleep() has been interrupted");
				}
			}

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onServiceReady();
				}
			});
			mThread = null;
		}
	}
}
