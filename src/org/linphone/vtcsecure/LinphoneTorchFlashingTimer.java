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
	
	@Override
	public void cancel() {
		super.cancel();
		terminate = true;
		if (torhcIsOn) {
			p.setFlashMode(Parameters.FLASH_MODE_OFF);
			cam.setParameters(p);
			cam.stopPreview();
		}
		//cam.release();
	}

	public void scheduleAtIntervalInSeconds(final Activity a, float periodInSeconds) {
		try {
			cam = Camera.open();
			p = cam.getParameters();
			p.setFlashMode(Parameters.FLASH_MODE_TORCH);
			cam.setParameters(p);
			cam.startPreview();
		} catch (Exception e) {
			Log.e("Failed tunrning torch on");
			return;
		}

		super.schedule(new TimerTask() {          
			@Override
			public void run() {
				if (torhcIsOn) p.setFlashMode(Parameters.FLASH_MODE_OFF);
				else if (!terminate) p.setFlashMode(Parameters.FLASH_MODE_TORCH);
				cam.setParameters(p);
				torhcIsOn = !torhcIsOn;	
			}
		}, 0, (long)(periodInSeconds*1000));

	}

}
