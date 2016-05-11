package org.linphone;
/*
AboutFragment.java
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

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.hockeyapp.android.UpdateManager;

import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;
import org.linphone.ui.EnterTextPopUpFragment;

/**
 * @author Sylvain Berfini
 */
public class AboutFragment extends Fragment implements OnClickListener, EnterTextPopUpFragment.EnterTextPopupListener {
	private FragmentsAvailable about = FragmentsAvailable.ABOUT_INSTEAD_OF_CHAT;
	View exitButton = null;
	View sendLogButton = null;
	View resetLogButton = null;

	EnterTextPopUpFragment passwordPopUp;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (getArguments() != null && getArguments().getSerializable("About") != null) {
			about = (FragmentsAvailable) getArguments().getSerializable("About");
		}

		View view = inflater.inflate(R.layout.about, container, false);

		TextView aboutText = (TextView) view.findViewById(R.id.AboutText);

		TextView buildIdText = (TextView)view.findViewById(R.id.buildId);

		try {
			aboutText.setText(String.format(getString(R.string.about_text), getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName));

			PackageManager manager = getActivity().getPackageManager();
			PackageInfo info = manager.getPackageInfo(
					getActivity().getPackageName(), 0);
			String version = String.valueOf(info.versionName);
			LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
			buildIdText.setText("Build: " +version+"\n"+"Core: "+lc.getVersion());

		} catch (NameNotFoundException e) {
			Log.e(e, "cannot get version name");
		}

		sendLogButton = view.findViewById(R.id.send_log);
		sendLogButton.setOnClickListener(this);
		sendLogButton.setVisibility(LinphonePreferences.instance().isDebugEnabled() ? View.VISIBLE : View.GONE);

		resetLogButton = view.findViewById(R.id.reset_log);
		resetLogButton.setOnClickListener(this);
		resetLogButton.setVisibility(LinphonePreferences.instance().isDebugEnabled() ? View.VISIBLE : View.GONE);

		exitButton = view.findViewById(R.id.exit);
		exitButton.setOnClickListener(this);
		exitButton.setVisibility(View.VISIBLE);

		((Button)view.findViewById(R.id.unlock_advanced_settings)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = LinphoneActivity.instance().getSupportFragmentManager();
				passwordPopUp = new EnterTextPopUpFragment();
				passwordPopUp.show(fm, "password_entry_popup");
				passwordPopUp.attachListener(AboutFragment.this);
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(about);

			if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
				LinphoneActivity.instance().hideStatusBar();
			}
		}


		checkForUpdates();
	}

	private void checkForUpdates() {
		// Remove this for store / production builds!
		UpdateManager.register(LinphoneActivity.instance(), "d6280d4d277d6876c709f4143964f0dc");
	}
	@Override
	public void onClick(View v) {
		if (LinphoneActivity.isInstanciated()) {
			LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
			if (v == sendLogButton) {
				//LinphoneUtils.collectLogs(LinphoneActivity.instance(), getString(R.string.about_bugreport_email));
				if (lc != null) {
					lc.uploadLogCollection();
				}
			} else if (v == resetLogButton) {
				if (lc != null) {
					lc.resetLogCollection();
				}
			} else {
				LinphoneActivity.instance().exit();
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	//If you are looking at this source code you are (should be) smart enough to handle these settings
	private static String advancedSettingsPW = "1234";
	@Override
	public void onPasswordSubmitted(String input) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());
		SharedPreferences.Editor editor = prefs.edit();
		if(input.equals(advancedSettingsPW)&&!prefs.getBoolean("advanced_settings_enabled",false)){//If password entered, and Advanced is not unlocked
			editor.putBoolean("advanced_settings_enabled", true);
			editor.commit();
			SettingsFragment.isAdvancedSettings = true;
			Toast.makeText(getActivity(), "Advanced settings unlocked", Toast.LENGTH_SHORT).show();
			LinphoneActivity.instance().displaySettings();
		}else if(input.equals(advancedSettingsPW)&&prefs.getBoolean("advanced_settings_enabled", false)){//If password entered, and Advanced is already unlocked
			editor.putBoolean("advanced_settings_enabled", false);
			editor.commit();
			SettingsFragment.isAdvancedSettings = false;
			Toast.makeText(getActivity(), "Advanced settings locked", Toast.LENGTH_SHORT).show();
			LinphoneActivity.instance().displaySettings();
		}else{//Wrong password
			Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
		}
	}
}
