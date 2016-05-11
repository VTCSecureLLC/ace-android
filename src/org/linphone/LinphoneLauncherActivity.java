/*
LinphoneLauncherActivity.java
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
package org.linphone;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.google.analytics.tracking.android.Tracker;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import org.linphone.custom.FontListParser;
import org.linphone.mediastream.Log;
import org.linphone.vtcsecure.GAUtils;
import org.linphone.vtcsecure.g;

import java.util.HashSet;
import java.util.List;

import static android.content.Intent.ACTION_MAIN;


/**
 * 
 * Launch Linphone main activity when Service is ready.
 * 
 * @author Guillaume Beraudo
 *
 */
public class LinphoneLauncherActivity extends Activity {

	private Handler mHandler;
	private ServiceWaitThread mThread;

	/**
	 * The {@link Tracker} used to record screen views.
	 */


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Used to change for the lifetime of the app the name used to tag the logs



		new Log(getResources().getString(R.string.app_name), !getResources().getBoolean(R.bool.disable_every_log));
		Log.TAG = "ACE";
		// Hack to avoid to draw twice LinphoneActivity on tablets


		//setContentView(R.layout.splash_screen);
		View view = LayoutInflater.from(this).inflate(R.layout.splash_screen, null);
		setContentView(view);
		view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
		mHandler = new Handler();


			if (LinphoneService.isReady()) {
				onServiceReady();
			} else {
				// start linphone as background
				startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
				mThread = new ServiceWaitThread();
				mThread.start();
			}

		initFontSettings();




	}

	protected void onServiceReady() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean hasAcceptedRelease = prefs.getBoolean("accepted_legal_release", false);
		final Class<? extends Activity> classToStart;
		if(!hasAcceptedRelease){
			classToStart = LegalRelease.class;
		}
		else {
			classToStart = /*LoginMainActivity.class;//*/LinphoneActivity.class;
		}
		LinphoneService.instance().setActivityToLaunchOnIncomingReceived(classToStart);
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				startActivity(new Intent().setClass(LinphoneLauncherActivity.this, classToStart).setData(getIntent().getData()));
				overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
				finish();
			}
		}, 1000);
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
	@Override
	protected void onResume() {
		super.onResume();
		//checkForCrashes();
		//checkForUpdates();
		g.analytics_tracker = new GAUtils(getApplicationContext()).getInstance(getApplicationContext());
		//Screen Hit
		//Log.i(Log.TAG, "Setting screen name: LauncherScreen");
		g.analytics_tracker.setScreenName("LauncherScreen");

		//Event
		g.analytics_tracker.send(this,"Action","App Launched",null,null);
	}
	@Override
	protected void onPause() {
		super.onPause();
		unregisterManagers();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterManagers();
	}
	private void checkForCrashes() {
		CrashManager.register(this, "d6280d4d277d6876c709f4143964f0dc");
	}

	private void checkForUpdates() {
		// Remove this for store / production builds!
		UpdateManager.register(this, "d6280d4d277d6876c709f4143964f0dc");
	}
	private void unregisterManagers() {
		UpdateManager.unregister();
		// unregister other managers if necessary...
	}

	private void initFontSettings(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);;

		if(prefs.getStringSet(getString(R.string.pref_text_settings_aviable_fonts), null) == null) {

			try {
				List<FontListParser.SystemFont> fonts = FontListParser.getSystemFonts();
				HashSet<String> fonts_collection = new HashSet<String>();

				for (FontListParser.SystemFont f : fonts) {
					Log.d("fonts avialable : " + f.name);
					fonts_collection.add(f.name);
				}
				prefs.edit().putStringSet(getString(R.string.pref_text_settings_aviable_fonts), fonts_collection).commit();

			} catch (Exception e) {
				HashSet<String> avoid_next_rum = new HashSet<String>();
				prefs.edit().putStringSet(getString(R.string.pref_text_settings_aviable_fonts), avoid_next_rum).commit();
				e.printStackTrace();
			}
		}
	}

}


