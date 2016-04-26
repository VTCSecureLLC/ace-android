package org.linphone;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreviewTorch extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;

	public CameraPreviewTorch(Context context, SurfaceView preview) {
		super(context);


		SurfaceHolder mHolder = preview.getHolder();
		mHolder.addCallback(this);
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		try {
			mCamera.setPreviewDisplay(mHolder);
		}catch(Throwable e){

		}
		torchOn();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//torchOff();
		Log.w("Camera surface destroy", "destroyed");
		if (mCamera != null) {
			Log.w("Camera surface destroy", "destroyed");
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}


	public void torchOn(){
		// Turn on LED
		if(mCamera!=null) {
			Camera.Parameters params = mCamera.getParameters();
			params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(params);
			mCamera.startPreview();
		}
	}

	public void torchOff(){
		// Turn off LED
		if(mCamera!=null) {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(params);
            //mCamera.stopPreview();
            //mCamera.release();
        }
	}

}