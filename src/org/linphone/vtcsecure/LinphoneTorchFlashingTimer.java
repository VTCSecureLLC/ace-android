package org.linphone.vtcsecure;

import java.util.Timer;
import java.util.TimerTask;

import org.linphone.mediastream.Log;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class LinphoneTorchFlashingTimer extends Timer {
	private  Camera cam;
	private  Parameters p;
	private boolean torhcIsOn = false;
	private boolean terminate = false;
	public static LinphoneTorchFlashingTimer instance;
	
	@Override
	public synchronized void cancel() {
		super.cancel();
		terminate = true;
		p.setFlashMode(Parameters.FLASH_MODE_OFF);
		cam.setParameters(p);
		cam.release();
		instance = null;
	}

	public void scheduleAtIntervalInSeconds(final Activity a, float periodInSeconds) {
		try {
			cam = Camera.open();
			p = cam.getParameters();
			p.setFlashMode(Parameters.FLASH_MODE_TORCH);
			cam.setParameters(p);
		} catch (Exception e) {
			Log.e("Failed tunrning torch on");
			return;
		}

		instance = this;
		super.schedule(new TimerTask() {          
			@Override
			public void run() {
				if (terminate) return;
				if (torhcIsOn) p.setFlashMode(Parameters.FLASH_MODE_OFF);
				else p.setFlashMode(Parameters.FLASH_MODE_TORCH);
				cam.setParameters(p);
				torhcIsOn = !torhcIsOn;	
			}
		}, 0, (long)(periodInSeconds*1000));

	}

}
