package org.linphone;
/*
VideoCallFragment.java
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
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.linphone.compatibility.Compatibility;
import org.linphone.compatibility.CompatibilityScaleGestureDetector;
import org.linphone.compatibility.CompatibilityScaleGestureListener;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;


/**
 * @author Sylvain Berfini
 */
public class VideoCallFragment extends Fragment implements OnGestureListener, OnDoubleTapListener, CompatibilityScaleGestureListener {

	private SurfaceView mVideoView;
	public static ImageView cameraCover;
	public static SurfaceView mCaptureView;
	private AndroidVideoWindowImpl androidVideoWindowImpl;
	private GestureDetector mGestureDetector;
	private float mZoomFactor = 1.f;
	private float mZoomCenterX, mZoomCenterY;
	private CompatibilityScaleGestureDetector mScaleDetector;
	private InCallActivity inCallActivity;
	private int dx,dy;
	private int viewId = R.layout.video;
	private SharedPreferences prefs;
	private boolean isSelfViewEnabled;

	private ObjectAnimator selfViewDownAnimation;
	private ObjectAnimator selfViewUpAnimation;
	float selfViewTranslation;
	boolean isMovied;

	@SuppressWarnings("deprecation") // Warning useless because value is ignored and automatically set by new APIs.

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		selfViewTranslation = dpToPx(150);



		LinphoneManager.getLc().setVideoDevice(CameraPreview.findFrontFacingCamera());
		prefs = PreferenceManager.
				getDefaultSharedPreferences(LinphoneActivity.instance());
		isSelfViewEnabled = prefs.getBoolean(getString(R.string.pref_av_show_self_view_key), true);
		isH263();
		View view = inflater.inflate(viewId, container, false);

		cameraCover = (ImageView) view.findViewById(R.id.cameraCover);
		mVideoView = (SurfaceView) view.findViewById(R.id.videoSurface);
		mCaptureView = (SurfaceView) view.findViewById(R.id.videoCaptureSurface);

		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mCaptureView.getLayoutParams();
		lp.bottomMargin = (int) selfViewTranslation;
		mCaptureView.setLayoutParams(lp);

		selfViewDownAnimation = ObjectAnimator.ofFloat(mCaptureView, "translationY", 0,selfViewTranslation);
		selfViewUpAnimation = ObjectAnimator.ofFloat(mCaptureView, "translationY", selfViewTranslation, 0);

		mCaptureView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Warning useless because value is ignored and automatically set by new APIs.

		// VTCSecure - Make the preview video draggable
		mCaptureView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				switch (motionEvent.getAction()) {
					case MotionEvent.ACTION_DOWN:
						dx = (int) motionEvent.getX();
						dy = (int) motionEvent.getY();
						break;
					case MotionEvent.ACTION_MOVE:
						isMovied = true;

						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mCaptureView.getLayoutParams();
						lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0); // Clears the rule, as there is no removeRule until API 17.
						lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);

						int x = (int) motionEvent.getX();
						int y = (int) motionEvent.getY();
						int left = lp.leftMargin + (x - dx);
						int top = lp.topMargin + (y - dy);
						lp.leftMargin = left;
						lp.topMargin = top;
						view.setLayoutParams(lp);
						break;
				}
				return true;
			}
		});

		if(isSelfViewEnabled){
			fixZOrder(mVideoView, mCaptureView);
		}

		if(!((InCallActivity)getActivity()).isCameraMuted())
		{
			LinphoneManager.getLc().setPreviewWindow(mCaptureView);
		}
		else
		{
			LinphoneManager.getLc().setPreviewWindow(null);
			mCaptureView.setVisibility(View.INVISIBLE);
		}

		androidVideoWindowImpl = new AndroidVideoWindowImpl(mVideoView, mCaptureView, new AndroidVideoWindowImpl.VideoWindowListener() {
			@Override
			public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
				if(!((InCallActivity) getActivity()).isCameraMuted())
					LinphoneManager.getLc().setVideoWindow(vw);
				mVideoView = surface;
			}

			@Override
			public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {
				LinphoneCore lc = LinphoneManager.getLc();
				if (lc != null) {
					lc.setVideoWindow(null);
				}
			}

			@Override
			public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
				mCaptureView = surface;
				//	isH263();
				((InCallActivity) getActivity()).invalidateSelfView(mCaptureView);
				if(!((InCallActivity) getActivity()).isCameraMuted())
					LinphoneManager.getLc().setPreviewWindow(mCaptureView);
			}

			@Override
			public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {
				// Remove references kept in jni code and restart camera
				LinphoneManager.getLc().setPreviewWindow(null);
			}
		});

		mVideoView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mScaleDetector != null) {
					mScaleDetector.onTouchEvent(event);
				}

				mGestureDetector.onTouchEvent(event);
				if (inCallActivity != null && event.getAction() == MotionEvent.ACTION_UP) {
					try {
						//If not screen buttons were pressed, toggle controls display
						if (inCallActivity.instance().mControlsLayout != null && inCallActivity.instance().mControlsLayout.getVisibility() != View.VISIBLE) {
							inCallActivity.instance().displayVideoCallControlsIfHidden(inCallActivity.NEVER);
						} else if (!inCallActivity.instance().animating_show_controls) {
							inCallActivity.instance().hide_controls(inCallActivity.NOW);
						}
					}
					catch(Exception e){
						Log.e("InCallActivity Null pointer", "target: mControlsLayout");
					}


					//inCallActivity.displayVideoCallControlsIfHidden(inCallActivity.NEVER);
				}
				return true;
			}
		});

		return view;
	}

	void handleSurfaceBounds(SurfaceView sv, int orientation, int screenLayout, boolean isH263) {
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) sv.getLayoutParams();
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM); // Clears the rule, as there is no removeRule until API 17.
		lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		lp.leftMargin = 0;
		lp.topMargin = 0;
		//resize will cause camera reinitialization
		sv.setLayoutParams(lp);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		boolean ish263 = isH263();
		handleSurfaceBounds(mCaptureView, newConfig.orientation, newConfig.screenLayout, ish263);
	}

	//Check to see if current call is using an H263 video codec, if so, adjust the video for portrait
	private boolean isH263(){
		LinphoneCallParams params;
		LinphoneCall call;

		LinphoneCore core = LinphoneManager.getLc();

		if(core != null) {
			call = core.getCurrentCall();
			if(call != null) {
				params = call.getCurrentParamsCopy();
				if(params.getUsedVideoCodec() != null) {
					if (params.getUsedVideoCodec().toString().contains("H263")) {

						adjustH263VideoForCall(call);

						return true;
					}
				}
			}
		}

		return false;
	}

	private void adjustH263VideoForCall(LinphoneCall call){

		Activity parent = getActivity();
		if(parent != null && parent.getWindowManager() != null) {

			int orientation = parent.getResources().getConfiguration().orientation;

			if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
				int currentRotation = parent.getWindowManager().getDefaultDisplay().getRotation();
				int newRotatation = currentRotation;

				if(currentRotation <= 180){
					newRotatation = 90;
				}

				else {
					newRotatation = 270;
				}
				LinphoneManager.getLc().setDeviceRotation(newRotatation);
				LinphoneManager.getLc().updateCall(call, null);
				viewId = R.layout.video_h263;
			}
		}

	}

	private void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);
		preview.setZOrderMediaOverlay(true); // Needed to be able to display control layout over
	}

	public void animateDownSelfView(boolean isAnimationDisabled)
	{
		if(mCaptureView == null)
			return;
		if(!isAnimationDisabled) {
			if (!isMovied && !selfViewDownAnimation.isRunning())
				selfViewDownAnimation.start();
		}
		else
			mCaptureView.setTranslationY(selfViewTranslation);

	}

	public void animateUpSelfView(boolean isAnimationDisabled)
	{
		if(mCaptureView == null)
			return;
		if(!isAnimationDisabled) {
			if (!isMovied && !selfViewUpAnimation.isRunning())
				selfViewUpAnimation.start();
			else
				mCaptureView.setTranslationY(0);
		}
	}

	public void switchCamera() {
		try {
			int videoDeviceId = LinphoneManager.getLc().getVideoDevice();
			videoDeviceId = (videoDeviceId + 1) % AndroidCameraConfiguration.retrieveCameras().length;
			LinphoneManager.getLc().setVideoDevice(videoDeviceId);
			CallManager.getInstance().updateCall();

			// previous call will cause graph reconstruction -> regive preview
			// window
			if (mCaptureView != null) {
				LinphoneManager.getLc().setPreviewWindow(mCaptureView);
			}
		} catch (ArithmeticException ae) {
			Log.e("Cannot swtich camera : no camera");
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mVideoView != null) {
			((GLSurfaceView) mVideoView).onResume();
		}

		if (androidVideoWindowImpl != null) {
			synchronized (androidVideoWindowImpl) {
				LinphoneManager.getLc().setVideoWindow(androidVideoWindowImpl);
			}
		}

		mGestureDetector = new GestureDetector(inCallActivity, this);
		mScaleDetector = Compatibility.getScaleGestureDetector(inCallActivity, this);
	}

	@Override
	public void onPause() {
		if (androidVideoWindowImpl != null) {
			synchronized (androidVideoWindowImpl) {
				/*
				 * this call will destroy native opengl renderer which is used by
				 * androidVideoWindowImpl
				 */
				LinphoneManager.getLc().setVideoWindow(null);
			}
		}

		if (mVideoView != null) {
			((GLSurfaceView) mVideoView).onPause();
		}

		super.onPause();
	}

	@Override
	public boolean onScale(CompatibilityScaleGestureDetector detector) {
		mZoomFactor *= detector.getScaleFactor();
		// Don't let the object get too small or too large.
		// Zoom to make the video fill the screen vertically
		float portraitZoomFactor = ((float) mVideoView.getHeight()) / (float) ((3 * mVideoView.getWidth()) / 4);
		// Zoom to make the video fill the screen horizontally
		float landscapeZoomFactor = ((float) mVideoView.getWidth()) / (float) ((3 * mVideoView.getHeight()) / 4);
		mZoomFactor = Math.max(0.1f, Math.min(mZoomFactor, Math.max(portraitZoomFactor, landscapeZoomFactor)));

		LinphoneCall currentCall = LinphoneManager.getLc().getCurrentCall();
		if (currentCall != null) {
			currentCall.zoomVideo(mZoomFactor, mZoomCenterX, mZoomCenterY);
			return true;
		}
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (LinphoneUtils.isCallEstablished(LinphoneManager.getLc().getCurrentCall())) {
			if (mZoomFactor > 1) {
				// Video is zoomed, slide is used to change center of zoom
				if (distanceX > 0 && mZoomCenterX < 1) {
					mZoomCenterX += 0.01;
				} else if(distanceX < 0 && mZoomCenterX > 0) {
					mZoomCenterX -= 0.01;
				}
				if (distanceY < 0 && mZoomCenterY < 1) {
					mZoomCenterY += 0.01;
				} else if(distanceY > 0 && mZoomCenterY > 0) {
					mZoomCenterY -= 0.01;
				}

				if (mZoomCenterX > 1)
					mZoomCenterX = 1;
				if (mZoomCenterX < 0)
					mZoomCenterX = 0;
				if (mZoomCenterY > 1)
					mZoomCenterY = 1;
				if (mZoomCenterY < 0)
					mZoomCenterY = 0;

				LinphoneManager.getLc().getCurrentCall().zoomVideo(mZoomFactor, mZoomCenterX, mZoomCenterY);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (LinphoneUtils.isCallEstablished(LinphoneManager.getLc().getCurrentCall())) {
			if (mZoomFactor == 1.f) {
				// Zoom to make the video fill the screen vertically
				float portraitZoomFactor = ((float) mVideoView.getHeight()) / (float) ((3 * mVideoView.getWidth()) / 4);
				// Zoom to make the video fill the screen horizontally
				float landscapeZoomFactor = ((float) mVideoView.getWidth()) / (float) ((3 * mVideoView.getHeight()) / 4);

				mZoomFactor = Math.max(portraitZoomFactor, landscapeZoomFactor);
			}
			else {
				resetZoom();
			}

			LinphoneManager.getLc().getCurrentCall().zoomVideo(mZoomFactor, mZoomCenterX, mZoomCenterY);
			return true;
		}

		return false;
	}

	private void resetZoom() {
		mZoomFactor = 1.f;
		mZoomCenterX = mZoomCenterY = 0.5f;
	}

	public float dpToPx(int dp) {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		float px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
		return px;
	}

	@Override
	public void onDestroy() {
		inCallActivity = null;

		mCaptureView = null;
		if (mVideoView != null) {
			mVideoView.setOnTouchListener(null);
			mVideoView = null;
		}
		if (androidVideoWindowImpl != null) {
			// Prevent linphone from crashing if correspondent hang up while you are rotating
			androidVideoWindowImpl.release();
			androidVideoWindowImpl = null;
		}
		if (mGestureDetector != null) {
			mGestureDetector.setOnDoubleTapListener(null);
			mGestureDetector = null;
		}
		if (mScaleDetector != null) {
			mScaleDetector.destroy();
			mScaleDetector = null;
		}

		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		inCallActivity = (InCallActivity) activity;
		if (inCallActivity != null) {
			inCallActivity.bindVideoFragment(this);
		}
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return true; // Needed to make the GestureDetector working
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
						   float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}


}
