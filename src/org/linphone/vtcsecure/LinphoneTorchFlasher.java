package org.linphone.vtcsecure;

import java.util.Timer;
import java.util.TimerTask;

import org.linphone.LinphonePreferences;
import org.linphone.mediastream.Log;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class LinphoneTorchFlasher extends Object {
	private  Camera cam;
	private  Parameters p;
	private boolean torhcIsOn = false;
	private boolean stopped = true;
	private Timer timer; 
	public static LinphoneTorchFlasher instance;
	
	
	public static synchronized LinphoneTorchFlasher instance() {
		if (instance == null ) {
			instance =  new LinphoneTorchFlasher();
		}
		return instance;
	}
	
	public synchronized void stopFlashTorch() {
		if (stopped) return;
		timer.cancel();
		timer = null;
		stopped = true;
		p.setFlashMode(Parameters.FLASH_MODE_OFF);
		cam.setParameters(p);
		cam.release();
	}
	
	public void startFlashTorch() {
		if (!stopped) return;
		float flashFrequencyInSeconds = LinphonePreferences.instance().getConfig().getFloat("vtcsecure", "incoming_flashlight_frequency", 0.3f);
		try {
			cam = Camera.open();
			p = cam.getParameters();
			p.setFlashMode(Parameters.FLASH_MODE_TORCH);
			cam.setParameters(p);
		} catch (Exception e) {
			Log.e("Failed tunrning torch on");
			return;
		}
		stopped = false;
		timer = new Timer();
		timer.schedule(new TimerTask() {          
			@Override
			public void run() {
				if (stopped) return;
				if (torhcIsOn) p.setFlashMode(Parameters.FLASH_MODE_OFF);
				else p.setFlashMode(Parameters.FLASH_MODE_TORCH);
				cam.setParameters(p);
				torhcIsOn = !torhcIsOn;	
			}
		}, 0, (long)(flashFrequencyInSeconds*1000));
	}

}
