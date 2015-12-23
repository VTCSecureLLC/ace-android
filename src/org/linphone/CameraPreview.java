package org.linphone;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private List<Camera.Size> mSupportedPreviewSizes;
	private Camera.Size mPreviewSize;

	public CameraPreview(Context context, Camera camera) {
		super(context);
		Log.d("CameraPreview constructor","constructor");
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("surfaceCreated", "surfaceCreated");
		try {
			// create the surface and start camera preview
			Log.d("mCamera",String.valueOf(mCamera));
			if (mCamera == null) {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			}
		} catch (Throwable e) {
			Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
		}
	}
	private int findFrontFacingCamera() {
		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
				//cameraFront = true;
				break;
			}
		}
		return cameraId;
	}
	public void refreshCamera(Camera camera) {
		Log.d("refreshCamera","refreshCamera");
			if (mHolder.getSurface() == null) {
				Log.d("mHolder.getSurface() == null","preview surface does not exist");
			// preview surface does not exist
			return;
		}
		// stop preview before making changes
		try {
			camera = Camera.open(findFrontFacingCamera());
		}catch(Throwable e){

		}
		camera.stopPreview();
		Log.d("mCamera.stopPreview()", "mCamera.stopPreview()");

		// set preview size and make any resize, rotate or
		// reformatting changes here
		// start preview with new settings

		Log.d("setCamera","setCamera");
		setCamera(camera);
		try {
			//mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
//			Camera.Parameters parameters = mCamera.getParameters();
//			parameters.setPreviewSize(100, 100);
//			mCamera.setParameters(parameters);
			Display display = ((WindowManager) LinphoneActivity.instance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			int rotation = display.getRotation();
			int degrees=0;
			Log.d("rotation", "rotation" + rotation);
			switch (rotation){
				case Surface.ROTATION_0:
					//tested on phone
					degrees=90;
					break;
				case Surface.ROTATION_90:
					//tested on nexus tab
					degrees=0;
					break;
				case Surface.ROTATION_180:
					//these are guesses
					degrees=180;
					break;
				case Surface.ROTATION_270:
					//these are guesses
					degrees=270;
					break;


			}

			camera.setDisplayOrientation(degrees);
			Log.d("mHolder", mHolder.toString());
			Log.d("mHolder.getSurface()",mHolder.getSurface().toString());
			camera.setPreviewDisplay(mHolder);
			Log.d("mCamera.setPreviewDisplay(mHolder);", "mCamera.setPreviewDisplay(mHolder);");
			camera.startPreview();
		} catch (Exception e) {
			Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
		}
	}
	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio=(double)h / w;

		if (sizes == null) return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}
//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
//		final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
//		setMeasuredDimension(width, height);
//
//		if (mSupportedPreviewSizes != null) {
//			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
//		}
//	}
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.
		Log.d("surfaceChanged","surfaceChanged");
		refreshCamera(mCamera);
	}

	public void setCamera(Camera camera) {
		//method to set a camera instance
		Log.d("setCamera","setCamera");
		mCamera = camera;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("Camera surface destroyed","destroyed");
		if (mCamera != null) {
			Log.d("Camera surface destroyed","destroyed");
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

	}
}