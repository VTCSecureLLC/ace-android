package org.linphone;

/*
SettingsFragment.java
Copyright (C) 2013  Belledonne Communications, Grenoble, France

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

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;

import net.hockeyapp.android.Constants;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.AdaptiveRateAlgorithm;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.MediaEncryption;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PayloadType;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.setup.SetupActivity;
import org.linphone.ui.LedPreference;
import org.linphone.ui.PreferencesListFragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class SettingsFragment extends PreferencesListFragment {

	//duplicate variables from AccountPreferencesFragment to duplicate functionality from that fragment for USM
	private int n;
	private boolean isNewAccount=false;
	private LinphonePreferences mPrefs;

	public static boolean isAdvancedSettings = false;

	private static final int WIZARD_INTENT = 1;
	private Handler mHandler = new Handler();
	private LinphoneCoreListenerBase mListener;

	SharedPreferences prefs;
	SharedPreferences.Editor editor;

	public SettingsFragment() {
		super(R.xml.preferences);
		mPrefs = LinphonePreferences.instance();
	}

	@Override
	public void onCreate(Bundle bundle) {
		if(mPrefs.getContext()==null)
			mPrefs.setContext(getActivity());
		if(LinphoneActivity.ctx == null)
			LinphoneActivity.ctx = getContext();
		super.onCreate(bundle);
		if(!LinphoneActivity.isInstanciated() || !LinphoneManager.isInstanciated())
		{
			return;
		}


		prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());

		if(prefs.getBoolean("advanced_settings_enabled",false)){
			isAdvancedSettings = true;
		}

		editor = prefs.edit();

		// Init the settings page interface
		initSettings();
		setListeners();
		hideSettings();
		
		mListener = new LinphoneCoreListenerBase(){
			@Override
			public void ecCalibrationStatus(LinphoneCore lc, final EcCalibratorStatus status, final int delayMs, Object data) {
				LinphoneManager.getInstance().routeAudioToReceiver();
				
				CheckBoxPreference echoCancellation = (CheckBoxPreference) findPreference(getString(R.string.pref_echo_cancellation_key));
				Preference echoCancellerCalibration = findPreference(getString(R.string.pref_echo_canceller_calibration_key));

				if (status == EcCalibratorStatus.DoneNoEcho) {
					echoCancellerCalibration.setSummary(R.string.no_echo);
					echoCancellation.setChecked(false);
					LinphonePreferences.instance().setEchoCancellation(false);
				} else if (status == EcCalibratorStatus.Done) {
					echoCancellerCalibration.setSummary(String.format(getString(R.string.ec_calibrated), delayMs));
					echoCancellation.setChecked(true);
					LinphonePreferences.instance().setEchoCancellation(true);
				} else if (status == EcCalibratorStatus.Failed) {
					echoCancellerCalibration.setSummary(R.string.failed);
					echoCancellation.setChecked(true);
					LinphonePreferences.instance().setEchoCancellation(true);
				}
			}
		};
	}

	// Inits the values or the listener on some settings
	private void initSettings() {
		initUSM();
		//Init accounts on Resume instead of on Create to update the account list when coming back from wizard
		initGeneralSettings();
		initAudioVideoSettings();
		initThemeSettings();
		initTextSettings();
		initSummarySettings();
		if(isAdvancedSettings) {
			initTunnelSettings();
			initAudioSettings();
			initVideoSettings();
			initCallSettings();
			initNetworkSettings();
			initAdvancedSettings();
		}
		// Add action on About button
		findPreference(getString(R.string.menu_about_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (LinphoneActivity.isInstanciated()) {
					LinphoneActivity.instance().displayAbout();
					return true;
				}
				return false;
			}
		});
		findPreference(getString(R.string.setup_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.logOutMessage)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										deleteDefaultAccount();
										Intent intent = new Intent(LinphoneService.instance(), SetupActivity.class);
										getActivity().startActivityForResult(intent, LinphoneActivity.FIRST_LOGIN_ACTIVITY);
									}
								}
						)
						.setNegativeButton(R.string.no,
								null
						)
						.create().show();
				return true;
			}
		});
		findPreference(getString(R.string.pref_add_account_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				int nbAccounts = mPrefs.getAccountCount();
				LinphoneActivity.instance().displayAccountSettings(nbAccounts);
				return true;
			}
		});
	}

	// Sets listener for each preference to update the matching value in linphonecore
	private void setListeners() {
		setGeneralPreferencesListener();
		setAudioVideoPreferencesListener();
		setThemePreferencesListener();
		if(isAdvancedSettings) {
			setTunnelPreferencesListener();
			setAudioPreferencesListener();
			setVideoPreferencesListener();
			setCallPreferencesListener();
			setNetworkPreferencesListener();
			setBackgroundThemePreferencesListener();
			setAdvancedPreferencesListener();
		}
	}

	// Read the values set in resources and hides the settings accordingly
	private void hideSettings() {
		if(!isAdvancedSettings) {
			emptyAndHidePreferenceCategory(R.string.pref_preferences);
			hidePreference(R.string.pref_video_enable_key);

			emptyAndHidePreferenceCategory(R.string.pref_tunnel_key);

			emptyAndHidePreferenceScreen(R.string.pref_audio);
			emptyAndHidePreferenceScreen(R.string.pref_video_key);
			emptyAndHidePreferenceScreen(R.string.call);
			emptyAndHidePreferenceScreen(R.string.pref_advanced);
			emptyAndHidePreferenceScreen(R.string.pref_network_title);
		}

		if (!getResources().getBoolean(R.bool.display_about_in_settings)) {
			hidePreference(R.string.menu_about_key);
		}

		if (getResources().getBoolean(R.bool.hide_accounts)) {
			emptyAndHidePreference(R.string.pref_sipaccounts_key);
		}

		if (getResources().getBoolean(R.bool.hide_wizard)){
			hidePreference(R.string.setup_key);
		}

		if(!getResources().getBoolean(R.bool.replace_wizard_with_old_interface)){
			hidePreference(R.string.pref_add_account_key);
		}

		if (getResources().getBoolean(R.bool.disable_animations)) {
			uncheckAndHidePreference(R.string.pref_animation_enable_key);
		}

		if (!getResources().getBoolean(R.bool.enable_linphone_friends)) {
			emptyAndHidePreference(R.string.pref_linphone_friend_key);
		}

		if (getResources().getBoolean(R.bool.disable_chat)) {
			findPreference(getString(R.string.pref_image_sharing_server_key)).setLayoutResource(R.layout.hidden);
		}

		if (!getResources().getBoolean(R.bool.enable_push_id)) {
			hidePreference(R.string.pref_push_notification_key);
		}

		if (!Version.isVideoCapable() || !LinphoneManager.getLcIfManagerNotDestroyedOrNull().isVideoSupported()) {
			uncheckAndHidePreference(R.string.pref_video_enable_key);
		} else {
			if (!AndroidCameraConfiguration.hasFrontCamera()) {
				uncheckAndHidePreference(R.string.pref_video_use_front_camera_key);
			}
		}

		if (!LinphoneManager.getLc().isTunnelAvailable()) {
			emptyAndHidePreference(R.string.pref_tunnel_key);
		}

		if (getResources().getBoolean(R.bool.hide_camera_settings)) {
			emptyAndHidePreference(R.string.pref_video_key);
			hidePreference(R.string.pref_video_enable_key);
		}

		if (getResources().getBoolean(R.bool.disable_every_log)) {
			uncheckAndHidePreference(R.string.pref_debug_key);
		}

		if (!LinphoneManager.getLc().upnpAvailable()) {
			uncheckAndHidePreference(R.string.pref_upnp_enable_key);
		}
	}

	private void uncheckAndHidePreference(int preferenceKey) {
		Preference preference = findPreference(getString(preferenceKey));
		if (!(preference instanceof CheckBoxPreference))
			return;

		CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
		checkBoxPreference.setChecked(false);
		hidePreference(checkBoxPreference);
	}

	private void emptyAndHidePreference(int preferenceKey) {
		Preference preference = findPreference(getString(preferenceKey));
		if (preference instanceof PreferenceCategory)
			emptyAndHidePreferenceCategory(preferenceKey);
		else if (preference instanceof PreferenceScreen)
			emptyAndHidePreferenceScreen(preferenceKey);
	}

	private void emptyAndHidePreferenceCategory(int preferenceKey) {
		Preference preference = findPreference(getString(preferenceKey));
		if (!(preference instanceof PreferenceCategory))
			return;

		PreferenceCategory preferenceCategory = (PreferenceCategory) preference;
		preferenceCategory.removeAll();
		hidePreference(preferenceCategory);
	}

	private void emptyAndHidePreferenceScreen(int preferenceKey) {
		Preference preference = findPreference(getString(preferenceKey));
		if (!(preference instanceof PreferenceScreen))
			return;

		PreferenceScreen preferenceScreen = (PreferenceScreen) preference;
		preferenceScreen.removeAll();
		hidePreference(preferenceScreen);
	}

	private void hidePreference(int preferenceKey) {
		hidePreference(findPreference(getString(preferenceKey)));
	}

	private void hidePreference(Preference preference) {
		if(preference != null) {
			preference.setLayoutResource(R.layout.hidden);
		}
	}

	private void setPreferenceDefaultValueAndSummary(int pref, String value) {
		if(value != null) {
			EditTextPreference etPref = (EditTextPreference) findPreference(getString(pref));
			etPref.setText(value);
			etPref.setSummary(value);
		}
	}

	private void initTunnelSettings() {
		setPreferenceDefaultValueAndSummary(R.string.pref_tunnel_host_key, mPrefs.getTunnelHost());
		setPreferenceDefaultValueAndSummary(R.string.pref_tunnel_port_key, String.valueOf(mPrefs.getTunnelPort()));
		ListPreference tunnelModePref = (ListPreference) findPreference(getString(R.string.pref_tunnel_mode_key));
		String tunnelMode = mPrefs.getTunnelMode();
		tunnelModePref.setSummary(tunnelMode);
		tunnelModePref.setValue(tunnelMode);
	}

	private void setTunnelPreferencesListener() {
		findPreference(getString(R.string.pref_tunnel_host_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String host = newValue.toString();
				mPrefs.setTunnelHost(host);
				preference.setSummary(host);
				return true;
			}
		});
		findPreference(getString(R.string.pref_tunnel_port_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				try {
					int port = Integer.parseInt(newValue.toString());
					mPrefs.setTunnelPort(port);
					preference.setSummary(String.valueOf(port));
					return true;
				} catch (NumberFormatException nfe) {
					return false;
				}
			}
		});
		findPreference(getString(R.string.pref_tunnel_mode_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String mode = newValue.toString();
				mPrefs.setTunnelMode(mode);
				preference.setSummary(mode);
				return true;
			}
		});
	}

	private void deleteAll()
	{
		PreferenceCategory accounts = (PreferenceCategory) findPreference(getString(R.string.pref_sipaccounts_key));
		accounts.removeAll();

		// Get already configured extra accounts
		int defaultAccountID = mPrefs.getDefaultAccountIndex();
		int nbAccounts = mPrefs.getAccountCount();
		for (int i = 0; i < nbAccounts; i++) {
			LinphonePreferences.instance().deleteAccount(i);
		}
	}
	private void deleteDefaultAccount(){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneManager.getInstance().getContext());
		LinphonePreferences mPrefs = LinphonePreferences.instance();
		int n= mPrefs.getDefaultAccountIndex();
		mPrefs.deleteAccount(n);
	}
	private void initAccounts() {
		PreferenceCategory accounts = (PreferenceCategory) findPreference(getString(R.string.pref_sipaccounts_key));
		accounts.removeAll();

		// Get already configured extra accounts
		int defaultAccountID = mPrefs.getDefaultAccountIndex();
		int nbAccounts = mPrefs.getAccountCount();
		for (int i = 0; i < nbAccounts; i++) {
			final int accountId = i;
			// For each, add menus to configure it
			String username = mPrefs.getAccountUsername(accountId);
			String domain = mPrefs.getAccountDomain(accountId);
			LedPreference account = new LedPreference(getActivity());

			if (username == null) {
				account.setTitle(getString(R.string.pref_sipaccount));
			} else {
				account.setTitle(username + "@" + domain);
			}

			if (defaultAccountID == i) {
				account.setSummary(R.string.default_account_flag);
			}

			account.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{
				public boolean onPreferenceClick(Preference preference) {
					LinphoneActivity.instance().displayAccountSettings(accountId);
					return false;
				}
			});
			updateAccountLed(account, username, domain, mPrefs.isAccountEnabled(i));
			accounts.addPreference(account);
		}
	}

	private void updateAccountLed(final LedPreference me, final String username, final String domain, boolean enabled) {
		if (!enabled) {
			me.setLed(R.drawable.led_disconnected);
			return;
		}

		if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
			for (LinphoneProxyConfig lpc : LinphoneManager.getLc().getProxyConfigList()) {
				LinphoneAddress addr = null;
				try {
					addr = LinphoneCoreFactory.instance().createLinphoneAddress(lpc.getIdentity());
				} catch (LinphoneCoreException e) {
					me.setLed(R.drawable.led_disconnected);
					return;
				}
				if (addr.getUserName().equals(username) && addr.getDomain().equals(domain)) {
					if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationOk) {
						me.setLed(R.drawable.led_connected);
					} else if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationFailed) {
						me.setLed(R.drawable.led_error);
					} else if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationProgress) {
						me.setLed(R.drawable.led_inprogress);
						mHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								updateAccountLed(me, username, domain, true);
							}
						}, 500);
					} else {
						me.setLed(R.drawable.led_disconnected);
					}
					break;
				}
			}
		}
	}

	private void initMediaEncryptionPreference(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add(getString(R.string.media_encryption_none));
		values.add(getString(R.string.pref_media_encryption_key_none));

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null || getResources().getBoolean(R.bool.disable_all_security_features_for_markets)) {
			setListPreferenceValues(pref, entries, values);
			return;
		}

		boolean hasZrtp = lc.mediaEncryptionSupported(MediaEncryption.ZRTP);
		boolean hasSrtp = lc.mediaEncryptionSupported(MediaEncryption.SRTP);
		boolean hasDtls = lc.mediaEncryptionSupported(MediaEncryption.DTLS);

		if (!hasSrtp && !hasZrtp && !hasDtls) {
			pref.setEnabled(false);
		} else {
			if (hasSrtp){
				entries.add(getString(R.string.media_encryption_srtp));
				values.add(getString(R.string.pref_media_encryption_key_srtp));
			}
			setListPreferenceValues(pref, entries, values);
		}

		MediaEncryption value = mPrefs.getMediaEncryption();
		pref.setSummary(value.toString());

		String key = getString(R.string.pref_media_encryption_key_none);
		if (value.toString().equals(getString(R.string.media_encryption_srtp))) {
			key = getString(R.string.pref_media_encryption_key_srtp);
		}
		pref.setValue(key);
	}

	private void initializeVideoPresetPreferences(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add("default");
		values.add("default");
		entries.add("high-fps");
		values.add("high-fps");
		entries.add("custom");
		values.add("custom");
		setListPreferenceValues(pref, entries, values);
		String value = "custom"; //samson mPrefs.getVideoPreset();
		pref.setSummary(value);
		pref.setValue(value);
	}

	private void initializeThemeColorPreferences(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add("Default");
		values.add("Default");
		entries.add("Red");
		values.add("Red");
		entries.add("Yellow");
		values.add("Yellow");
		entries.add("Gray");
		values.add("Gray");
		entries.add("High Visibility");
		values.add("High Visibility");
		entries.add("Tech");
		values.add("Tech");
		entries.add("Custom");
		values.add("Custom");
		setListPreferenceValues(pref, entries, values);
		String value =prefs.getString(getResources().getString(R.string.pref_theme_app_color_key), "Tech");
		pref.setSummary(value);
		pref.setValue(value);

	}

	private void initializeBackgroundThemeColorPreferences(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add("Default");
		values.add("Default");
		entries.add("Red");
		values.add("Red");
		entries.add("Yellow");
		values.add("Yellow");
		entries.add("Gray");
		values.add("Gray");
		entries.add("High Visibility");
		values.add("High Visibility");
		entries.add("Custom");
		values.add("Custom");
		setListPreferenceValues(pref, entries, values);
		String value =prefs.getString(getResources().getString(R.string.pref_theme_background_color_key), "Default");
		pref.setSummary(value);
		pref.setValue(value);

	}

	private void initializePreferredVideoSizePreferences(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		for (String name : LinphoneManager.getLc().getSupportedVideoSizes()) {
			entries.add(name);
			values.add(name);
		}

		setListPreferenceValues(pref, entries, values);

		String value = mPrefs.getPreferredVideoSize();
		pref.setSummary(value);
		pref.setValue(value);
	}

	private void initializePreferredVideoFpsPreferences(ListPreference pref) {
		List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		entries.add("none");
		values.add("0");
		for (int i = 5; i <= 30; i += 5) {
			String str = Integer.toString(i);
			entries.add(str);
			values.add(str);
		}
		setListPreferenceValues(pref, entries, values);
		String value = Integer.toString(mPrefs.getPreferredVideoFps());
		if (value.equals("0")) {
			mPrefs.setPreferredVideoFps(30);
			value = "30";
		}
		pref.setSummary(value);
		pref.setValue(value);
	}

	private static void setListPreferenceValues(ListPreference pref, List<CharSequence> entries, List<CharSequence> values) {
		CharSequence[] contents = new CharSequence[entries.size()];
		entries.toArray(contents);
		pref.setEntries(contents);
		contents = new CharSequence[values.size()];
		values.toArray(contents);
		pref.setEntryValues(contents);
	}

	private void initAudioSettings() {
		PreferenceCategory codecs = (PreferenceCategory) findPreference(getString(R.string.pref_codecs_key));
		codecs.removeAll();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		for (final PayloadType pt : lc.getAudioCodecs()) {
			CheckBoxPreference codec = new CheckBoxPreference(getActivity());
			codec.setTitle(pt.getMime());
			/* Special case */
			if (pt.getMime().equals("mpeg4-generic")) {
				if (android.os.Build.VERSION.SDK_INT < 16) {
					/* Make sure AAC is disabled */
					try {
						lc.enablePayloadType(pt, false);
					} catch (LinphoneCoreException e) {
						e.printStackTrace();
					}
					continue;
				} else {
					codec.setTitle("AAC-ELD");
				}
			}

			codec.setSummary(pt.getRate() + " Hz");
			codec.setChecked(lc.isPayloadTypeEnabled(pt));

			codec.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean enable = (Boolean) newValue;
					try {
						LinphoneManager.getLcIfManagerNotDestroyedOrNull().enablePayloadType(pt, enable);
					} catch (LinphoneCoreException e) {
						e.printStackTrace();
					}
					return true;
				}
			});

			codecs.addPreference(codec);
		}

		CheckBoxPreference echoCancellation = (CheckBoxPreference) findPreference(getString(R.string.pref_echo_cancellation_key));
		echoCancellation.setChecked(mPrefs.isEchoCancellationEnabled());

		if (mPrefs.isEchoCancellationEnabled()) {
			Preference echoCalibration = findPreference(getString(R.string.pref_echo_canceller_calibration_key));
			echoCalibration.setSummary(String.format(getString(R.string.ec_calibrated), mPrefs.getEchoCalibration()));
		}

		CheckBoxPreference adaptiveRateControl = (CheckBoxPreference) findPreference(getString(R.string.pref_adaptive_rate_control_key));
		adaptiveRateControl.setChecked(mPrefs.isAdaptiveRateControlEnabled());

		ListPreference adaptiveRateAlgorithm = (ListPreference) findPreference(getString(R.string.pref_adaptive_rate_algorithm_key));
		adaptiveRateAlgorithm.setSummary(String.valueOf(mPrefs.getAdaptiveRateAlgorithm()));
		adaptiveRateAlgorithm.setValue(String.valueOf(mPrefs.getAdaptiveRateAlgorithm()));

		ListPreference bitrateLimit = (ListPreference) findPreference(getString(R.string.pref_codec_bitrate_limit_key));
		bitrateLimit.setSummary(String.valueOf(mPrefs.getCodecBitrateLimit()));
		bitrateLimit.setValue(String.valueOf(mPrefs.getCodecBitrateLimit()));
	}

	private void setAudioPreferencesListener() {
		findPreference(getString(R.string.pref_echo_cancellation_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enabled = (Boolean) newValue;
				mPrefs.setEchoCancellation(enabled);
				return true;
			}
		});

		findPreference(getString(R.string.pref_adaptive_rate_control_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enabled = (Boolean) newValue;
				mPrefs.enableAdaptiveRateControl(enabled);
				return true;
			}
		});

		findPreference(getString(R.string.pref_adaptive_rate_algorithm_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setAdaptiveRateAlgorithm(AdaptiveRateAlgorithm.fromString((String) newValue));
				preference.setSummary(String.valueOf(mPrefs.getAdaptiveRateAlgorithm()));
				return true;
			}
		});


		findPreference(getString(R.string.pref_codec_bitrate_limit_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setCodecBitrateLimit(Integer.parseInt(newValue.toString()));
				LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
				int bitrate = Integer.parseInt(newValue.toString());

				for (final PayloadType pt : lc.getAudioCodecs()) {
					if (lc.payloadTypeIsVbr(pt)) {
						lc.setPayloadTypeBitrate(pt, bitrate);
					}
				}

				preference.setSummary(String.valueOf(mPrefs.getCodecBitrateLimit()));
				return true;
			}
		});

		findPreference(getString(R.string.pref_echo_canceller_calibration_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				synchronized (SettingsFragment.this) {
					try {
						LinphoneManager.getInstance().startEcCalibration(mListener);
						preference.setSummary(R.string.ec_calibrating);
					} catch (LinphoneCoreException e) {
						Log.w(e, "Cannot calibrate EC");
					}
				}
				return true;
			}
		});
	}

	private void setBackgroundThemePreferencesListener() {
//		findPreference(getString(R.string.pref_theme_background_color_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			@Override
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//
//				String color = prefs.getString(getString(R.string.pref_theme_background_color_key), "Default");
//
//				preference.setSummary(newValue.toString());
//				editor.putString(getString(R.string.pref_theme_background_color_key), newValue.toString());
//				editor.commit();
//				LinphoneActivity.setBackgroundColorTheme(LinphoneActivity.ctx);
//				return true;
//			}
//		});
//

	}
	private void initUSM(){
		//Added code from AccountPreferencesFragment to help reproduce functionality.
		n = mPrefs.getDefaultAccountIndex();
		if(n == mPrefs.getAccountCount()){
			isNewAccount=true;
		} else {
		}
	}
	private void initGeneralSettings(){
		((CheckBoxPreference)findPreference(getString(R.string.pref_autostart_key))).setChecked(mPrefs.isAutoStartEnabled());

//		boolean isSipEncryptionEnabled = false; //VATRP-1007
//		((CheckBoxPreference)findPreference(getString(R.string.pref_general_sip_encryption_key))).setChecked(isSipEncryptionEnabled);

		((CheckBoxPreference) findPreference(getString(R.string.pref_wifi_only_key))).setChecked(mPrefs.isWifiOnlyEnabled());

		CheckBoxPreference autoAnswer = (CheckBoxPreference) findPreference(getString(R.string.pref_auto_answer_key));
		boolean auto_answer = prefs.getBoolean(getString(R.string.pref_auto_answer_key), this.getResources().getBoolean(R.bool.auto_answer_calls));

		if (auto_answer) {
			autoAnswer.setChecked(true);
			autoAnswer.setEnabled(true);
			editor.putBoolean(getString(R.string.pref_auto_answer_key), true);
			editor.commit();
		} else {
			autoAnswer.setChecked(false);
			autoAnswer.setEnabled(true);
			editor.putBoolean(getString(R.string.pref_auto_answer_key), false);
			editor.commit();
		}


		//if(isAdvancedSettings) {
		Preference resetPreerence = findPreference(getString(R.string.pref_reset_key));
		resetPreerence.setTitle(R.string.reset);
		resetPreerence.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				new AlertDialog.Builder(getActivity())
						.setMessage(R.string.reset_question)
						.setPositiveButton(R.string.button_ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										AceApplication.getInstance().clearApplicationData();
										Intent mStartActivity = new Intent(getActivity(), LinphoneLauncherActivity.class);
										int mPendingIntentId = 123456;
										PendingIntent mPendingIntent = PendingIntent.getActivity(getActivity(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
										AlarmManager mgr = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
										mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, mPendingIntent);
										LinphoneActivity.instance().exit();
									}
								}
						)
						.setNegativeButton(R.string.button_cancel,
								null
						)
						.create().show();


				return true;
			}
		});

		//}
	}

	private void setGeneralPreferencesListener(){
		findPreference(getString(R.string.pref_autostart_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				mPrefs.setAutoStart(value);
				return true;
			}
		});

//
//		findPreference(getString(R.string.pref_general_sip_encryption_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			@Override
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//				boolean value = (Boolean) newValue;
//				if(value){
//					mPrefs.setAccountTransport(n, getString(R.string.pref_transport_tls_key));
//					mPrefs.setAccountProxy(n, mPrefs.getAccountProxy(n).replace("5060","5061"));
//				}else{
//					mPrefs.setAccountTransport(n, getString(R.string.pref_transport_tcp_key));
//					mPrefs.setAccountProxy(n, mPrefs.getAccountProxy(n).replace("5061","5060"));
//				}
//				return true;
//			}
//		});

		findPreference(getString(R.string.pref_wifi_only_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setWifiOnlyEnabled((Boolean) newValue);
				return true;
			}
		});

	}

	private void initAudioVideoSettings(){
		String rtcpFeedbackMode = prefs.getString(getString(R.string.pref_av_rtcp_feedback_key), "Off");
		((ListPreference) findPreference(getString(R.string.pref_av_rtcp_feedback_key))).setValue(rtcpFeedbackMode);
		((ListPreference) findPreference(getString(R.string.pref_av_rtcp_feedback_key))).setSummary(rtcpFeedbackMode);

		boolean isCameraMuted = prefs.getBoolean(getString(R.string.pref_av_camera_mute_key), false);
		((CheckBoxPreference) findPreference(getString(R.string.pref_av_camera_mute_key))).setChecked(isCameraMuted);

		// VATRP-1017 -- Add global speaker and mic mute logic
		boolean isSpeakerMuted = prefs.getBoolean(getString(R.string.pref_av_speaker_mute_key), false);
		((CheckBoxPreference) findPreference(getString(R.string.pref_av_speaker_mute_key))).setChecked(isSpeakerMuted);

		boolean isMicMuted = prefs.getBoolean(getString(R.string.pref_av_mute_mic_key), false);
		((CheckBoxPreference)findPreference(getString(R.string.pref_av_mute_mic_key))).setChecked(isMicMuted);
		//
		CheckBoxPreference echoCancellation = (CheckBoxPreference) findPreference(getString(R.string.pref_echo_cancellation_key));
		echoCancellation.setChecked(mPrefs.isEchoCancellationEnabled());

		if (mPrefs.isEchoCancellationEnabled()) {
			Preference echoCalibration = findPreference(getString(R.string.pref_echo_canceller_calibration_key));
			echoCalibration.setSummary(String.format(getString(R.string.ec_calibrated), mPrefs.getEchoCalibration()));
		}

		SharedPreferences prefs = PreferenceManager.
				getDefaultSharedPreferences(LinphoneActivity.instance());

			//Todo: VATRP-1019 -- Add self view toggle
		String selfVideoIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_self_view_key);
		boolean isSelfViewEnabled = prefs.getBoolean(selfVideoIsEnabledKey, true);
		((CheckBoxPreference)findPreference(getString(R.string.pref_av_show_self_view_key))).setChecked(isSelfViewEnabled);

		//VATRP-1020 Add global camera preview toggle
		String previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
		boolean isPreviewEnabled = prefs.getBoolean(previewIsEnabledKey, true);
		((CheckBoxPreference)findPreference(getString(R.string.pref_av_show_preview_key))).setChecked(isPreviewEnabled);
	}

	private void setAudioVideoPreferencesListener(){
		findPreference(getString(R.string.pref_av_rtcp_feedback_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String) newValue;
				if (value.compareToIgnoreCase("Off") == 0) {
					LinphoneManager.getLc().getDefaultProxyConfig().enableAvpf(false);
					LinphoneManager.getLc().getConfig().setInt("rtp", "rtcp_fb_implicit_rtcp_fb", 0);
				} else if (value.compareToIgnoreCase("Implicit") == 0) {
					LinphoneManager.getLc().getDefaultProxyConfig().enableAvpf(false);
					LinphoneManager.getLc().getConfig().setInt("rtp", "rtcp_fb_implicit_rtcp_fb", 1);
				} else if (value.compareToIgnoreCase("Explicit") == 0) {
					LinphoneManager.getLc().getDefaultProxyConfig().enableAvpf(true);
					LinphoneManager.getLc().getConfig().setInt("rtp", "rtcp_fb_implicit_rtcp_fb", 1);
				}
				LinphoneManager.getLc().getDefaultProxyConfig().setAvpfRRInterval(3);
				try{
					preference.setSummary(newValue.toString());
				}
				catch(Throwable e){
					e.printStackTrace();
					Log.e("RTCP selected:", "Invalid option");
				}
				return true;
			}
		});
		//VATRP-1017 -- Add global speaker and mic mute logic
		findPreference(getString(R.string.pref_av_speaker_mute_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				prefs.edit().putBoolean(getString(R.string.pref_av_speaker_mute_key), value).commit();
				return true;
			}
		});
		findPreference(getString(R.string.pref_av_mute_mic_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				LinphoneManager.getLc().muteMic(value);
				prefs.edit().putBoolean(getString(R.string.pref_av_mute_mic_key), value).commit();
				return true;
			}
		});
		//
		findPreference(getString(R.string.pref_echo_cancellation_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enabled = (Boolean) newValue;
				mPrefs.setEchoCancellation(enabled);
				return true;
			}
		});

		findPreference(getString(R.string.pref_echo_canceller_calibration_key)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				synchronized (SettingsFragment.this) {
					try {
						LinphoneManager.getInstance().startEcCalibration(mListener);
						preference.setSummary(R.string.ec_calibrating);
					} catch (LinphoneCoreException e) {
						Log.w(e, "Cannot calibrate EC");
					}
				}
				return true;
			}
		});
		//
		//Todo: VATRP-1019 -- Add self view toggle
		findPreference(getString(R.string.pref_av_show_self_view_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				SharedPreferences prefs = PreferenceManager.
						getDefaultSharedPreferences(LinphoneActivity.instance());
				String selfVideoIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_self_view_key);
				prefs.edit().putBoolean(selfVideoIsEnabledKey, value).commit();
				return true;
			}
		});
		//Todo: VATRP-1020 Add global camera preview toggle
		findPreference(getString(R.string.pref_av_show_preview_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;

				SharedPreferences prefs = PreferenceManager.
						getDefaultSharedPreferences(LinphoneActivity.instance());
				String previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
				prefs.edit().putBoolean(previewIsEnabledKey, value).commit();
				return true;
			}
		});
	}


	private void initThemeSettings() {
		final SharedPreferences prefs = PreferenceManager.
				getDefaultSharedPreferences(LinphoneActivity.instance());
		//initializeThemeColorPreferences((ListPreference) findPreference(getString(R.string.pref_theme_app_color_key)));
		//initializeBackgroundThemeColorPreferences((ListPreference) findPreference(getString(R.string.pref_theme_background_color_key)));

		((Preference)findPreference(getString(R.string.pref_theme_foreground_color_setting_key))).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				//Todo: VATRP-1022 -- Add foreground / background color picker
								//Black, blue, cyan, grey, green, magenda
				int[] colors = {Color.argb(220, 0, 0, 0), Color.argb(200, 0, 50, 150), Color.argb(200, 0, 160, 160), Color.argb(200, 50, 50, 50),
						Color.argb(200, 0, 160, 50), Color.argb(200, 160, 0, 150), Color.argb(200, 160, 0, 0),
						Color.argb(200, 255, 255, 255), Color.argb(200, 160, 160, 0)};

				int selectedColor = prefs.getInt(getString(R.string.pref_theme_foreground_color_setting_key), Color.RED);
				ColorPickerDialog dialog = ColorPickerDialog.newInstance(R.string.color_picker_foreground_title,
						colors, selectedColor, colors.length, colors.length);
				dialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
					@Override
					public void onColorSelected(int color) {
						prefs.edit().putInt(getString(R.string.pref_theme_foreground_color_setting_key), color).commit();
						LinphoneActivity.instance().setColorTheme(getActivity());
					}
				});
				dialog.show(getFragmentManager(), "COLOR_PICKER");

				return true;
			}
		});
		((Preference)findPreference(getString(R.string.pref_theme_background_color_setting_key))).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				//Black, blue, cyan, grey, green, magenda
				int[] colors = {Color.argb(220, 0, 0, 0), Color.argb(200, 0, 50, 150), Color.argb(200, 0, 160, 160), Color.argb(200, 10, 10, 10),
						Color.argb(200, 0, 160, 50), Color.argb(200, 160, 0, 150), Color.argb(200, 160, 0, 0),
						Color.argb(200, 255, 255, 255), Color.argb(200, 160, 160, 0)};

				int selectedColor = prefs.getInt(getString(R.string.pref_theme_background_color_setting_key), Color.RED);
				ColorPickerDialog dialog = ColorPickerDialog.newInstance(R.string.color_picker_background_title,
						colors, selectedColor, colors.length, colors.length);
				dialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
					@Override
					public void onColorSelected(int color) {
						prefs.edit().putInt(getString(R.string.pref_theme_background_color_setting_key), color).commit();
						LinphoneActivity.instance().setBackgroundColorTheme(getActivity());
					}
				});
				dialog.show(getFragmentManager(), "COLOR_PICKER");

				return true;
			}
		});

		//Todo: VATRP-1024 Add 508 compliance logic
		((CheckBoxPreference)findPreference(getString(R.string.pref_theme_force_508_key))).setChecked(false);

	}


	private void setThemePreferencesListener() {


//		findPreference(getString(R.string.pref_theme_app_color_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			@Override
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//
//				String color = prefs.getString(getString(R.string.pref_theme_app_color_key), "Tech");
//
//				preference.setSummary(newValue.toString());
//				editor.putString(getString(R.string.pref_theme_app_color_key), newValue.toString());
//				editor.commit();
//				LinphoneActivity.setColorTheme(LinphoneActivity.ctx);
//				return true;
//			}
//		});

	};

	private void initSummarySettings(){
		((Preference)findPreference(getString(R.string.pref_summary_view_tss_key))).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				createAndDisplayTSS();
				return true;
			}
		});
	}

	private void createAndDisplayTSS(){
		String version;
		try {
			PackageManager manager = getActivity().getPackageManager();
			PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
			version  = String.valueOf(info.versionName);
		}

		catch(PackageManager.NameNotFoundException e){
			version = "Beta";
		}


		String tssContents = "\n\n DEVICE INFO:\n\n" + Constants.PHONE_MANUFACTURER
				+ " " + Constants.PHONE_MODEL + "\nAndroid:" + Constants.ANDROID_VERSION + " " + "\nACE v" + version + "\n\n" +
				"CONFIG: \n\n" + getConfigSettingsAsString();
		File feedback = null;
		try
		{
			File root = new File(Environment.getExternalStorageDirectory(), "ACE");
			if (!root.exists()) {
				root.mkdirs();
			}
			feedback = new File(root, "tss.txt");
			FileWriter writer = new FileWriter(feedback);
			writer.append(tssContents);
			writer.flush();
			writer.close();
			Toast.makeText(LinphoneActivity.ctx, "Saved", Toast.LENGTH_SHORT).show();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		if(feedback != null) {
			///storage/emulated/0/ACE/tss.txt
			File crashFeedbackFile = new File(Environment.getExternalStorageDirectory() +"/ACE/tss.txt");
			Intent target = new Intent(Intent.ACTION_VIEW);
			target.setDataAndType(Uri.fromFile(crashFeedbackFile), MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt"));
			target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

			Intent pickerActivity = Intent.createChooser(target, "View Technical Support Sheet with...");
			startActivity(pickerActivity);
		}
	}

	private String getConfigSettingsAsString(){
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if(lc == null) return "LinphoneCore = null";
		if(lc.getDefaultProxyConfig() == null) return "config = null";

		String config = "";
		ArrayList<String> values = new ArrayList<String>();
		/**Account**/
		String accountHeader = "\nACCOUNT: \n";
		values.add(accountHeader);
		/**Username**/
		String username = "username = " + lc.getDefaultProxyConfig().getAddress().getUserName();
		values.add(username);
		/**Domain**/
		String domain = "domain = " + lc.getDefaultProxyConfig().getDomain();
		values.add(domain);
		/**Transport**/
		String transport = "transport = "+lc.getDefaultProxyConfig().getAddress().getTransport().toString();
		values.add(transport);
		/**Port**/
		String port = "port = " + String.valueOf(lc.getDefaultProxyConfig().getAddress().getPort());
		values.add(port);
		/**Proxy**/
		String proxy = "proxy = " + lc.getDefaultProxyConfig().getProxy();
		values.add(proxy);
		/**Full Address**/
		String fullAddr = "full_address = " + lc.getDefaultProxyConfig().getAddress().asStringUriOnly();
		values.add(fullAddr);

		/**Video**/
		String videoHeader = "\nVIDEO: \n";
		values.add(videoHeader);
		/**Video codecs**/
		for (final PayloadType pt : lc.getVideoCodecs()) {
			try {
				String isCodecEnabled = pt.getMime()+" = " + String.valueOf(lc.isPayloadTypeEnabled(pt));
				values.add(isCodecEnabled);
				String codecSendFmtp = pt.getMime() + " send-fmtp = " + pt.getSendFmtp();
				String codecRecvFmtp = pt.getMime() + " recv-fmtp = " + pt.getRecvFmtp();
				values.add(codecSendFmtp);
				values.add(codecRecvFmtp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/**Audio**/
		String audioHeader = "\nAUDIO: \n";
		values.add(audioHeader);
		/**Audio codecs**/
		for (final PayloadType pt : lc.getAudioCodecs()) {
			try {
				String isCodecEnabled = pt.getMime()+" = " + String.valueOf(lc.isPayloadTypeEnabled(pt));
				values.add(isCodecEnabled);
				String codecSendFmtp = pt.getMime() + " send-fmtp = " + pt.getSendFmtp();
				String codecRecvFmtp = pt.getMime() + " recv-fmtp = " + pt.getRecvFmtp();
				values.add(codecSendFmtp);
				values.add(codecRecvFmtp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//MISC settings
		String miscHeader = "\nMISC: \n";
		values.add(miscHeader);

		String isVideoEnabled = "video_enabled = " + String.valueOf(lc.isVideoEnabled());
		values.add(isVideoEnabled);

		/**Camera Mute**/
		String isCameraMuted = "camera_mute = " + String.valueOf(prefs.getBoolean(getString(R.string.pref_av_camera_mute_key), false));
		values.add(isCameraMuted);

		/**Mute**/
		String isMicMuted = "mic_mute = " + String.valueOf(prefs.getBoolean(getString(R.string.pref_av_mute_mic_key), false));
		values.add(isMicMuted);
		String isSpeakerMuted = "speaker_mute = " + String.valueOf(prefs.getBoolean(getString(R.string.pref_av_speaker_mute_key), false));
		values.add(isSpeakerMuted);

		//Echo cancellation
		String echoCancel = "echo_cancellation = " + String.valueOf(lc.isEchoCancellationEnabled());
		values.add(echoCancel);
		//Adaptive rate control
		String adaptiveRateControl = "adaptive_rate_control = " + String.valueOf(lc.isAdaptiveRateControlEnabled());
		values.add(adaptiveRateControl);
		//STUN
		String stun = lc.getStunServer();
		if(stun == null) { stun = "none"; }
		String stunVal = "stun = " + stun;
		values.add(stunVal);

		//AVPF
		String AVPF = "avpf = " + lc.getDefaultProxyConfig().avpfEnabled();
		values.add(AVPF);

		String avpfRRInterval = "avpf_rr_interval = "+ String.valueOf(lc.getDefaultProxyConfig().getAvpfRRInterval());
		values.add(avpfRRInterval);

		String rtcpFeedback = "rtcp_feedback = " + String.valueOf(LinphoneManager.getLc().getConfig().getInt("rtp", "rtcp_fb_implicit_rtcp_fb", 0));
		values.add(rtcpFeedback);
		//Video size
		String preferredVideoSize = "preferred_video_size = " + lc.getPreferredVideoSize().toDisplayableString();
		values.add(preferredVideoSize);
		//Bandwidth
		String downloadBW = "download_bandwidth = " + String.valueOf(lc.getDownloadBandwidth());
		values.add(downloadBW);

		String uploadBW = "upload_bandwidth = " + String.valueOf(lc.getUploadBandwidth());
		values.add(uploadBW);

		for(int i = 0; i < values.size(); i++){
			config = config + values.get(i) + "\n";
		}
		return config;
	}
	private void createTSS() {
		String version;
		try {
			PackageManager manager = getActivity().getPackageManager();
			PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
			version = String.valueOf(info.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			version = "Beta";
		}

		String tssContents = "\n\n DEVICE INFO:\n\n" + Constants.PHONE_MANUFACTURER
				+ " " + Constants.PHONE_MODEL + "\nAndroid:" + Constants.ANDROID_VERSION + " " + "\nACE v" + version + "\n\n" +
				"CONFIG: \n\n" + getConfigSettingsAsString();
		File feedback = null;
		try {
			File root = new File(Environment.getExternalStorageDirectory(), "ACE");
			if (!root.exists()) {
				root.mkdirs();
			}
			feedback = new File(root, "tss.txt");
			FileWriter writer = new FileWriter(feedback);
			writer.append(tssContents);
			writer.flush();
			writer.close();
			Toast.makeText(LinphoneActivity.ctx, "Saved", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initTextSettings() {
		Log.d("RTT: initTextSettings()");
		CheckBoxPreference enableTextCb = (CheckBoxPreference)findPreference(getString(R.string.pref_text_enable_key));

		boolean isTextEnabled = prefs.getBoolean(getString(R.string.pref_text_enable_key), true);
		Log.d("RTT: RTT enabled from earlier? " + isTextEnabled);
		enableTextCb.setChecked(prefs.getBoolean(getString(R.string.pref_text_enable_key), true));

		enableTextCb.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object value) {
				boolean enabled = (Boolean) value;
				Log.d("RTT: onPreferenceChange(), enabled: " + enabled);
				editor.putBoolean(getString(R.string.pref_text_enable_key), enabled);
				return true;
			}
		});

		//ListPreference
		ListPreference text_send_type_pref = (ListPreference)findPreference(getString(R.string.pref_text_settings_send_mode_key));

		//Values accepted are RTT or SIP_SIMPLE
		text_send_type_pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object value) {
				Log.d("text_send_type_pref value", value);
				editor.putString(getString(R.string.pref_text_settings_send_mode_key), value.toString());
				try {
					preference.setSummary(value.toString().replace("_", " "));
				}catch(Throwable e){

				}
				return true;
			}
		});

		String value=text_send_type_pref.getValue();
		try {
			text_send_type_pref.setSummary(value.toString().replace("_", " "));
		}catch(Throwable e) {
			//field is still blank.
		}
	}

	private void initVideoSettings() {
		initializeVideoPresetPreferences((ListPreference) findPreference(getString(R.string.pref_video_preset_key)));
		initializePreferredVideoSizePreferences((ListPreference) findPreference(getString(R.string.pref_preferred_video_size_key)));
		initializePreferredVideoFpsPreferences((ListPreference) findPreference(getString(R.string.pref_preferred_video_fps_key)));
		EditTextPreference bandwidth = (EditTextPreference) findPreference(getString(R.string.pref_bandwidth_limit_key));
		bandwidth.setText(Integer.toString(mPrefs.getBandwidthLimit()));
		bandwidth.setSummary(bandwidth.getText());
		updateVideoPreferencesAccordingToPreset();

		PreferenceCategory codecs = (PreferenceCategory) findPreference(getString(R.string.pref_video_codecs_key));
		codecs.removeAll();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		for (final PayloadType pt : lc.getVideoCodecs()) {
			if(pt.getMime().equals("MP4V-ES")||pt.getMime().equals("H263-1998")){
				continue;
			}
			CheckBoxPreference codec = new CheckBoxPreference(getActivity());
			codec.setTitle(pt.getMime());

			if (!pt.getMime().equals("VP8")) {
				if (getResources().getBoolean(R.bool.disable_all_patented_codecs_for_markets)) {
					continue;
				} else {
					if (!Version.hasFastCpuWithAsmOptim() && pt.getMime().equals("H264"))
					{
						// Android without neon doesn't support H264
						Log.w("CPU does not have asm optimisations available, disabling H264");
						continue;
					}
				}
			}
			codec.setChecked(lc.isPayloadTypeEnabled(pt));

			codec.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean enable = (Boolean) newValue;
					try {
						LinphoneManager.getLcIfManagerNotDestroyedOrNull().enablePayloadType(pt, enable);
					} catch (LinphoneCoreException e) {
						e.printStackTrace();
					}
					return true;
				}
			});

			codecs.addPreference(codec);
		}

		((CheckBoxPreference) findPreference(getString(R.string.pref_video_enable_key))).setChecked(mPrefs.isVideoEnabled());
		((CheckBoxPreference) findPreference(getString(R.string.pref_video_use_front_camera_key))).setChecked(mPrefs.useFrontCam());
		((CheckBoxPreference) findPreference(getString(R.string.pref_video_initiate_call_with_video_key))).setChecked(mPrefs.shouldInitiateVideoCall());
		//((CheckBoxPreference) findPreference(getString(R.string.pref_video_automatically_share_my_video_key))).setChecked(mPrefs.shouldAutomaticallyShareMyVideo());
		((CheckBoxPreference) findPreference(getString(R.string.pref_video_automatically_accept_video_key))).setChecked(mPrefs.shouldAutomaticallyAcceptVideoRequests());
	}

	private void updateVideoPreferencesAccordingToPreset() {
		if (mPrefs.getVideoPreset().equals("custom")) {
			findPreference(getString(R.string.pref_preferred_video_fps_key)).setEnabled(true);
			findPreference(getString(R.string.pref_bandwidth_limit_key)).setEnabled(true);
		} else {
			findPreference(getString(R.string.pref_preferred_video_fps_key)).setEnabled(false);
			findPreference(getString(R.string.pref_bandwidth_limit_key)).setEnabled(false);
		}
		((ListPreference) findPreference(getString(R.string.pref_video_preset_key))).setSummary(mPrefs.getVideoPreset());
		int fps = mPrefs.getPreferredVideoFps();
		String fpsStr = Integer.toString(fps);
		if (fpsStr.equals("0")) {
			mPrefs.setPreferredVideoFps(30);
			fpsStr = "30";
		}
		((ListPreference) findPreference(getString(R.string.pref_preferred_video_fps_key))).setSummary(fpsStr);
		((EditTextPreference) findPreference(getString(R.string.pref_bandwidth_limit_key))).setSummary(Integer.toString(mPrefs.getBandwidthLimit()));
	}

	private void setVideoPreferencesListener() {
		findPreference(getString(R.string.pref_av_camera_mute_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				prefs.edit().putBoolean(getString(R.string.pref_av_camera_mute_key), value).commit();
				return true;
			}
		});
		findPreference(getString(R.string.pref_video_enable_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enable = (Boolean) newValue;
				mPrefs.enableVideo(enable,enable);
				return true;
			}
		});

		findPreference(getString(R.string.pref_video_use_front_camera_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enable = (Boolean) newValue;
				mPrefs.setFrontCamAsDefault(enable);
				return true;
			}
		});

		findPreference(getString(R.string.pref_video_initiate_call_with_video_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enable = (Boolean) newValue;
				mPrefs.setInitiateVideoCall(enable);
				return true;
			}
		});

		/*
		findPreference(getString(R.string.pref_video_automatically_share_my_video_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enable = (Boolean) newValue;
				mPrefs.setAutomaticallyShareMyVideo(enable);
				return true;
			}
		});
		*/

		findPreference(getString(R.string.pref_video_automatically_accept_video_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean enable = (Boolean) newValue;
				mPrefs.setAutomaticallyAcceptVideoRequests(enable);
				return true;
			}
		});

		findPreference(getString(R.string.pref_video_preset_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setVideoPreset(newValue.toString());
				preference.setSummary(mPrefs.getVideoPreset());
				updateVideoPreferencesAccordingToPreset();
				return true;
			}
		});
		findPreference(getString(R.string.pref_preferred_video_size_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setPreferredVideoSize(newValue.toString());
				preference.setSummary(mPrefs.getPreferredVideoSize());
				updateVideoPreferencesAccordingToPreset();
				return true;
			}
		});

		findPreference(getString(R.string.pref_preferred_video_fps_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setPreferredVideoFps(Integer.parseInt(newValue.toString()));
				updateVideoPreferencesAccordingToPreset();
				return true;
			}
		});

		findPreference(getString(R.string.pref_bandwidth_limit_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setBandwidthLimit(Integer.parseInt(newValue.toString()));
				preference.setSummary(newValue.toString());
				return true;
			}
		});
	}

	private void initCallSettings() {
		CheckBoxPreference rfc2833 = (CheckBoxPreference) findPreference(getString(R.string.pref_rfc2833_dtmf_key));
		CheckBoxPreference autoAnswer = (CheckBoxPreference) findPreference(getString(R.string.pref_auto_answer_key));
		CheckBoxPreference sipInfo = (CheckBoxPreference) findPreference(getString(R.string.pref_sipinfo_dtmf_key));

		if (mPrefs.useRfc2833Dtmfs()) {
			rfc2833.setChecked(true);
			sipInfo.setChecked(false);
			sipInfo.setEnabled(false);
		} else if (mPrefs.useSipInfoDtmfs()) {
			sipInfo.setChecked(true);
			rfc2833.setChecked(false);
			rfc2833.setEnabled(false);
		}


		boolean auto_answer = prefs.getBoolean(getString(R.string.pref_auto_answer_key), this.getResources().getBoolean(R.bool.auto_answer_calls));

		if (auto_answer) {
			autoAnswer.setChecked(true);
			autoAnswer.setEnabled(true);
			editor.putBoolean(getString(R.string.pref_auto_answer_key), true);
			editor.commit();
		} else {
			autoAnswer.setChecked(false);
			autoAnswer.setEnabled(true);
			editor.putBoolean(getString(R.string.pref_auto_answer_key), false);
			editor.commit();
		}


		setPreferenceDefaultValueAndSummary(R.string.pref_voice_mail_key, mPrefs.getVoiceMailUri());
		setPreferenceDefaultValueAndSummary(R.string.pref_mail_waiting_indicator_key,
				prefs.getString(getString(R.string.pref_mail_waiting_indicator_key), ""));
	}

	private void setCallPreferencesListener() {
		findPreference(getString(R.string.pref_rfc2833_dtmf_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean use = (Boolean) newValue;
				CheckBoxPreference sipInfo = (CheckBoxPreference) findPreference(getString(R.string.pref_sipinfo_dtmf_key));
				sipInfo.setEnabled(!use);
				sipInfo.setChecked(false);
				mPrefs.sendDtmfsAsRfc2833(use);
				return true;
			}
		});

		findPreference(getString(R.string.pref_voice_mail_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				EditTextPreference voiceMail = (EditTextPreference) findPreference(getString(R.string.pref_voice_mail_key));
				voiceMail.setSummary(newValue.toString());
				voiceMail.setText(newValue.toString());
				mPrefs.setVoiceMailUri(newValue.toString());
				return true;
			}
		});

		findPreference(getString(R.string.pref_mail_waiting_indicator_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				EditTextPreference mwiUri= (EditTextPreference) findPreference(getString(R.string.pref_mail_waiting_indicator_key));
				mwiUri.setSummary(newValue.toString());
				mwiUri.setText(newValue.toString());
				prefs.edit().putString(getString(R.string.pref_mail_waiting_indicator_key), newValue.toString()).commit();
				return true;
			}
		});
		findPreference(getString(R.string.pref_sipinfo_dtmf_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean use = (Boolean) newValue;
				CheckBoxPreference rfc2833 = (CheckBoxPreference) findPreference(getString(R.string.pref_rfc2833_dtmf_key));
				rfc2833.setEnabled(!use);
				rfc2833.setChecked(false);
				mPrefs.sendDTMFsAsSipInfo(use);
				return true;
			}
		});
	}

	private void initNetworkSettings() {
		initMediaEncryptionPreference((ListPreference) findPreference(getString(R.string.pref_media_encryption_key)));

		((CheckBoxPreference) findPreference(getString(R.string.pref_wifi_only_key))).setChecked(mPrefs.isWifiOnlyEnabled());

		// Disable UPnP if ICE si enabled, or disable ICE if UPnP is enabled
		CheckBoxPreference ice = (CheckBoxPreference) findPreference(getString(R.string.pref_ice_enable_key));
		CheckBoxPreference upnp = (CheckBoxPreference) findPreference(getString(R.string.pref_upnp_enable_key));
		ice.setChecked(mPrefs.isIceEnabled());
		if (mPrefs.isIceEnabled()) {
			upnp.setEnabled(false);
		} else {
			upnp.setChecked(mPrefs.isUpnpEnabled());
			if (mPrefs.isUpnpEnabled()) {
				ice.setEnabled(false);
			}
		}

		CheckBoxPreference randomPort = (CheckBoxPreference) findPreference(getString(R.string.pref_transport_use_random_ports_key));
		randomPort.setChecked(mPrefs.isUsingRandomPort());

//		// Disable sip port choice if port is random
//		EditTextPreference sipPort = (EditTextPreference) findPreference(getString(R.string.pref_sip_port_key));
//		sipPort.setEnabled(!randomPort.isChecked());
//		sipPort.setSummary(mPrefs.getSipPort());
//		sipPort.setText(mPrefs.getSipPort());

		EditTextPreference stun = (EditTextPreference) findPreference(getString(R.string.pref_stun_server_key));
		stun.setSummary(mPrefs.getStunServer());
		stun.setText(mPrefs.getStunServer());

		((CheckBoxPreference) findPreference(getString(R.string.pref_push_notification_key))).setChecked(mPrefs.isPushNotificationEnabled());
		((CheckBoxPreference) findPreference(getString(R.string.pref_ipv6_key))).setChecked(mPrefs.isUsingIpv6());
	}

	private void setNetworkPreferencesListener() {
		findPreference(getString(R.string.pref_wifi_only_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setWifiOnlyEnabled((Boolean) newValue);
				return true;
			}
		});

		findPreference(getString(R.string.pref_stun_server_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setStunServer(newValue.toString());
				preference.setSummary(newValue.toString());
				return true;
			}
		});

		findPreference(getString(R.string.pref_ice_enable_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				CheckBoxPreference upnp = (CheckBoxPreference) findPreference(getString(R.string.pref_upnp_enable_key));
				boolean value = (Boolean) newValue;
				upnp.setChecked(false);
				upnp.setEnabled(!value);
				mPrefs.setIceEnabled((Boolean) newValue);
				return true;
			}
		});

		findPreference(getString(R.string.pref_upnp_enable_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				CheckBoxPreference ice = (CheckBoxPreference) findPreference(getString(R.string.pref_ice_enable_key));
				boolean value = (Boolean) newValue;
				ice.setChecked(false);
				ice.setEnabled(!value);
				mPrefs.setUpnpEnabled(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_transport_use_random_ports_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean randomPort = (Boolean) newValue;
				mPrefs.useRandomPort((Boolean) newValue);
				findPreference(getString(R.string.pref_sip_port_key)).setEnabled(!randomPort);
				return true;
			}
		});

//		findPreference(getString(R.string.pref_sip_port_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			@Override
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//				int port = -1;
//				try {
//					port = Integer.parseInt(newValue.toString());
//				} catch (NumberFormatException nfe) { }
//
//				mPrefs.setSipPort(port);
//				preference.setSummary(newValue.toString());
//				return true;
//			}
//		});

		findPreference(getString(R.string.pref_media_encryption_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = newValue.toString();
				MediaEncryption menc = MediaEncryption.None;
				if (value.equals(getString(R.string.pref_media_encryption_key_srtp)))
					menc = MediaEncryption.SRTP;
				else if (value.equals(getString(R.string.pref_media_encryption_key_zrtp)))
					menc = MediaEncryption.ZRTP;
				else if (value.equals(getString(R.string.pref_media_encryption_key_dtls)))
					menc = MediaEncryption.DTLS;
				mPrefs.setMediaEncryption(menc);

				preference.setSummary(mPrefs.getMediaEncryption().toString());
				return true;
			}
		});

		findPreference(getString(R.string.pref_push_notification_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.setPushNotificationEnabled((Boolean) newValue);
				return true;
			}
		});

		findPreference(getString(R.string.pref_ipv6_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPrefs.useIpv6((Boolean) newValue);
				return true;
			}
		});
	}

	private void initAdvancedSettings() {
		((CheckBoxPreference)findPreference(getString(R.string.pref_debug_key))).setChecked(mPrefs.isDebugEnabled());
		((CheckBoxPreference)findPreference(getString(R.string.pref_background_mode_key))).setChecked(mPrefs.isBackgroundModeEnabled());
		((CheckBoxPreference)findPreference(getString(R.string.pref_animation_enable_key))).setChecked(mPrefs.areAnimationsEnabled());
		((CheckBoxPreference)findPreference(getString(R.string.pref_autostart_key))).setChecked(mPrefs.isAutoStartEnabled());
		setPreferenceDefaultValueAndSummary(R.string.pref_image_sharing_server_key, mPrefs.getSharingPictureServerUrl());
		setPreferenceDefaultValueAndSummary(R.string.pref_remote_provisioning_key, mPrefs.getRemoteProvisioningUrl());
		setPreferenceDefaultValueAndSummary(R.string.pref_display_name_key, mPrefs.getDefaultDisplayName());
		setPreferenceDefaultValueAndSummary(R.string.pref_user_name_key, mPrefs.getDefaultUsername());
	}

	private void setAdvancedPreferencesListener() {
		findPreference(getString(R.string.pref_debug_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				mPrefs.setDebugEnabled(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_background_mode_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				mPrefs.setBackgroundModeEnabled(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_animation_enable_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				mPrefs.setAnimationsEnabled(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_autostart_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean value = (Boolean) newValue;
				mPrefs.setAutoStart(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_image_sharing_server_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String) newValue;
				mPrefs.setSharingPictureServerUrl(value);
				preference.setSummary(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_remote_provisioning_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String) newValue;
				mPrefs.setRemoteProvisioningUrl(value);
				preference.setSummary(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_display_name_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String) newValue;
				mPrefs.setDefaultDisplayName(value);
				preference.setSummary(value);
				return true;
			}
		});

		findPreference(getString(R.string.pref_user_name_key)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String) newValue;
				if(value.equals("")) return false;

				mPrefs.setDefaultUsername(value);
				preference.setSummary(value);
				return true;
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.SETTINGS);

			if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
				LinphoneActivity.instance().hideStatusBar();
			}
		}

		if(isAdvancedSettings){
			setPreferenceScreen(null);
			addPreferencesFromResource(R.xml.preferences);
			initSettings();
			setListeners();
			hideSettings();
		}

		initAccounts();
	}


}
