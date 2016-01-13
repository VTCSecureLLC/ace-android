package org.linphone;
/*
DialerFragment.java
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.setup.ApplicationPermissionManager;
import org.linphone.ui.AddressAware;
import org.linphone.ui.AddressText;
import org.linphone.ui.CallButton;
import org.linphone.ui.EraseButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class DialerFragment extends Fragment {

	public OrientationEventListener mOrientationHelper;
	public Camera mCamera;
	public static Camera.Size optimal_preview_size;

	private static DialerFragment instance;
	private static boolean isCallTransferOngoing = false;
	public CameraPreview mPreview;
	private Context myContext;
	public boolean mVisible;
	private AddressText mAddress;
	private CallButton mCall;
	private ImageView mAddContact;
	public LinearLayout cameraPreview;
	private AndroidVideoWindowImpl androidVideoWindowImpl;
	private OnClickListener addContactListener, cancelListener, transferListener;
	private boolean shouldEmptyAddressField = true;
	private boolean userInteraction = false;
	//private Camera camera;
	public View dialer_content;

	String color_theme;
	String background_color_theme;
	View dialer_view;
	private boolean cameraFront = false;
	int SELF_VIEW_INDEX = 0, DIALER_INDEX = 1;
	public int VIEW_INDEX = DIALER_INDEX;

	private boolean isSpinnerOpen = false;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		instance = this;
		final View view = inflater.inflate(R.layout.dialer, container, false);

		dialer_content = view.findViewById(R.id.dialerContent);



		dialer_view=view;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.ctx);
		color_theme = prefs.getString(LinphoneActivity.ctx.getResources().getString(R.string.pref_theme_app_color_key), "Tech");
		background_color_theme = prefs.getString(LinphoneActivity.ctx.getResources().getString(R.string.pref_theme_background_color_key), "default");


		mAddress = (AddressText) view.findViewById(R.id.Adress); 
		mAddress.setDialerFragment(this);

		int camera = CameraPreview.findFrontFacingCamera();
		if(camera == -1){
			camera = 0;
		}
		LinphoneManager.getLc().setVideoDevice(camera);
		// VTCSecure SIP Domain selection 
		final Spinner sipDomainSpinner = (Spinner)view.findViewById(R.id.sipDomainSpinner);

		final TextView sipDomainTextView = (TextView)view.findViewById(R.id.sipDomainTextView);
		sipDomainTextView.setText("");
		String externalDomains = LinphonePreferences.instance().getConfig().getString("vtcsecure", "external_domains", "");
		if (externalDomains.length()>0) {
			externalDomains =","+externalDomains;
			String sipDomains[] = externalDomains.split(",");
			final List<String> sipDomainsList=new ArrayList<String>(Arrays.asList(sipDomains));
			final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),android.R.layout.simple_spinner_item, sipDomainsList);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sipDomainSpinner.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(event.getAction() == MotionEvent.ACTION_DOWN){
						isSpinnerOpen = !isSpinnerOpen;
						if(isSpinnerOpen){
							new AlertDialog.Builder(DialerFragment.this.getActivity())
									.setTitle("")
									.setMessage("Available in General Release")
									.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											isSpinnerOpen = false;
										}
									}).show();
						}
					}
					return false;
				}
			});

//			sipDomainSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
//				public void onItemSelected(AdapterView<?> parent, View spinnerView, int position, long id) {
//					try {
//						if (position == 0) {
//							//set background gray because we are using the @ symbol
//							((LinearLayout) dialer_view.findViewById(R.id.provider_spinner_box)).setBackgroundColor(getResources().getColor(R.color.background_color));
//						} else {
//							((LinearLayout) dialer_view.findViewById(R.id.provider_spinner_box)).setBackgroundColor(getResources().getColor(R.color.text_color));
//						}
//					} catch (Throwable e) {
//						//crashing on tablets because dialer_view or provider_spinner_box is missing
//					}
//					if (position != 0) sipDomainTextView.setText("@" + adapter.getItem(position));
//					else sipDomainTextView.setText("");
//					mAddress.setTag(sipDomainTextView.getText());
//				}
//
//				public void onNothingSelected(AdapterView<?> arg0) {
//				}
//			});
			sipDomainSpinner.setAdapter(new SpinnerAdapter(getActivity(), R.layout.spiner_ithem,
					new String[]{"","Sorenson VRS", "ZVRS", "CAAG", "Purple VRS", "Global VRS",	"Convo Relay"},
					new int[]{R.drawable.atbutton_new,R.drawable.provider_logo_sorenson,
							R.drawable.provider_logo_zvrs,
							R.drawable.provider_logo_caag,//caag
							R.drawable.provider_logo_purplevrs,
							R.drawable.provider_logo_globalvrs,//global
							R.drawable.provider_logo_convorelay}));

		} else {
			sipDomainSpinner.setVisibility(View.GONE);
		}

		EraseButton erase = (EraseButton) view.findViewById(R.id.Erase);
		erase.setAddressWidget(mAddress);

		mCall = (CallButton) view.findViewById(R.id.Call);

		//Make text to right of call button clickable.
		TextView call_button_text = (TextView)view.findViewById(R.id.call_button_text);
		call_button_text.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCall.performClick();

			}
		});

		mCall.setAddressWidget(mAddress);
		if (LinphoneActivity.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0) {
			if (isCallTransferOngoing) {
				mCall.setImageResource(R.drawable.transfer_call);
			} else {
				mCall.setImageResource(R.drawable.add_call);
			}
		} else {
			mCall.setImageResource(R.drawable.call_button_new);
		}

		AddressAware numpad = (AddressAware) view.findViewById(R.id.Dialer);
		if (numpad != null) {
			numpad.setAddressWidget(mAddress);
		}

		mAddContact = (ImageView) view.findViewById(R.id.addContact);

		addContactListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinphoneActivity.instance().displayContactsForEdition(mAddress.getText().toString());
			}
		};
		cancelListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			}
		};
		transferListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinphoneCore lc = LinphoneManager.getLc();
				if (lc.getCurrentCall() == null) {
					return;
				}
				lc.transferCall(lc.getCurrentCall(), mAddress.getText().toString());
				isCallTransferOngoing = false;
				LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			}
		};

		mAddContact.setEnabled(!(LinphoneActivity.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0));
		resetLayout(isCallTransferOngoing);

		if (getArguments() != null) {
			shouldEmptyAddressField = false;
			String number = getArguments().getString("SipUri");
			String displayName = getArguments().getString("DisplayName");
			String photo = getArguments().getString("PhotoUri");
			mAddress.setText(number);
			if (displayName != null) {
				mAddress.setDisplayedName(displayName);
			}
			if (photo != null) {
				mAddress.setPictureUri(Uri.parse(photo));
			}
		}


		mAddress.setBackgroundResource(R.drawable.dialer_address_background_new);
		//sipDomainSpinner.setBackgroundResource(R.drawable.atbutton);
		erase.setImageResource(R.drawable.backspace_new);
		mAddContact.setImageResource(R.drawable.add_contact_new);


		//set background color independent

		view.setBackgroundResource(R.drawable.background_theme_new);



//		try {
//			if (!LinphoneActivity.instance().isTablet()) {
//				isPreviewEnabled = true;
//			}
//		}catch(Throwable e){
//			Log.e("Trying to check if device is tablet, but linphoneactivity isn't instanciated yet\n"+e.getMessage());
//		}
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myContext = getActivity().getApplication().getBaseContext();
        
        startOrientationSensor();

	return view;
}
	public void  initialize_camera(){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.ctx);
		String previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
		boolean isPreviewEnabled = prefs.getBoolean(previewIsEnabledKey, true);
		try {
			if(ApplicationPermissionManager.isPermissionGranted(getActivity(), Manifest.permission.CAMERA)&&isPreviewEnabled)
				initialize_camera(dialer_view);
		}catch(Throwable e){

		}
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void initialize_camera(View view) {
		cameraPreview = (LinearLayout) view.findViewById(R.id.camera_preview);
		cameraPreview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(dialer_content != null) {
					dialer_content.setVisibility(View.VISIBLE);
				}
				VIEW_INDEX = DialerFragment.instance().SELF_VIEW_INDEX;
			}
		});


		Log.d("mCamera" + mCamera);

		mPreview = new CameraPreview(myContext, mCamera);
		cameraPreview.addView(mPreview);
		//cameraPreview.addOnLayoutChangeListener(camera_view_listener());

	}

	//	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
//	public View.OnLayoutChangeListener camera_view_listener(){
//		View.OnLayoutChangeListener camera_view_listener=new View.OnLayoutChangeListener() {
//			@Override
//			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//
//				try {
//					cameraPreview = (LinearLayout) dialer_view.findViewById(R.id.camera_preview);
//					cameraPreview.setOnClickListener(new OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							if (dialer_content != null) {
//								dialer_content.setVisibility(View.VISIBLE);
//							}
//							VIEW_INDEX = DialerFragment.instance().SELF_VIEW_INDEX;
//						}
//					});
//
//					try {
//						mCamera = Camera.open(findFrontFacingCamera());
//					} catch (Throwable e) {
//						e.printStackTrace();
//						Log.d("couldn't open front camera");
//					}
//					Log.d("mCamera" + mCamera);
//
//					mPreview = new CameraPreview(myContext, mCamera);
//					cameraPreview.addView(mPreview);
//					cameraPreview.addOnLayoutChangeListener(camera_view_listener());
//					List<Camera.Size> mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
//					int viewWidth = mPreview.getWidth();
//					int viewHeight = mPreview.getHeight();
//					Log.d("mPreview" + mPreview.getWidth() + " " + mPreview.getHeight());
//					Camera.Parameters parameters = mCamera.getParameters();
//					optimal_preview_size = getOptimalPreviewSize(mSupportedPreviewSizes, viewWidth, viewHeight);
//					parameters.setPreviewSize(optimal_preview_size.width, optimal_preview_size.height);
//					mCamera.setParameters(parameters);
//				}catch(Throwable e){
//					e.printStackTrace();
//				}
//			}
//		};
//		return camera_view_listener;
//	};
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

//		private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
//			final double ASPECT_TOLERANCE = 0.1;
//			double targetRatio=(double)h / w;
//
//			if (sizes == null) return null;
//
//			Camera.Size optimalSize = null;
//			double minDiff = Double.MAX_VALUE;
//
//			int targetHeight = h;
//
//			for (Camera.Size size : sizes) {
//				double ratio = (double) size.width / size.height;
//				if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
//				if (Math.abs(size.height - targetHeight) < minDiff) {
//					optimalSize = size;
//					minDiff = Math.abs(size.height - targetHeight);
//				}
//			}
//
//			if (optimalSize == null) {
//				minDiff = Double.MAX_VALUE;
//				for (Camera.Size size : sizes) {
//					if (Math.abs(size.height - targetHeight) < minDiff) {
//						optimalSize = size;
//						minDiff = Math.abs(size.height - targetHeight);
//					}
//				}
//			}
//			return optimalSize;
//		}

    /**
	 * @return null if not ready yet
	 */
	public static DialerFragment instance() { 
		return instance;
	}

	@Override
	public void onPause() {
		super.onPause();
		if(mPreview!=null){
		mPreview.surfaceDestroyed(null);
		}
		//releaseCamera();
//		if (androidVideoWindowImpl != null) {
//			synchronized (androidVideoWindowImpl) {
//				/*
//				 * this call will destroy native opengl renderer which is used by
//				 * androidVideoWindowImpl
//				 */
//				LinphoneManager.getLc().setVideoWindow(null);
//			}
//		}
	}

//	private void releaseCamera() {
//		// stop and release camera
//		if (mCamera != null) {
//			mCamera.release();
//			mCamera = null;
//		}
//	}

	private synchronized void startOrientationSensor() {
		//Disable global orientation change listener, and initiate only dialer listener
		LinphoneActivity.instance().mOrientationHelper.disable();
		if (mOrientationHelper == null) {
			mOrientationHelper = new LocalOrientationEventListener(LinphoneActivity.instance());
		}
		mOrientationHelper.enable();
	}
	public int mAlwaysChangingPhoneAngle = -1;
	public int lastDeviceAngle = 0;
	private class LocalOrientationEventListener extends OrientationEventListener {
		public LocalOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(final int o) {

			if (o == OrientationEventListener.ORIENTATION_UNKNOWN) {
				return;
			}


			//alpha is the distance from each axis we want to allow a rotation.
			int degrees;

			degrees = lastDeviceAngle;

			int sensativity = 10;


			if (o < 0 + sensativity || o > 360 - sensativity)// when o is around 0, we set degrees to zero
				degrees = 0;
			else if (o > 90 - sensativity && o < 90 + sensativity)//when o is around 90, we set the degrees to 90
				degrees = 90;
			else if (o > 180 - sensativity && o < 180 + sensativity)//when o is around 180 we set the degrees to 180
				degrees = 180;
			else if (o > 270 - sensativity && o < 270 + sensativity)//when o is around 180 we set the degrees to 180
				degrees = 270;

			/*Log.d("onOrientationChanged_Dialer", o);
			Log.d("degrees", degrees);
			Log.d("mAlwaysChangingPhoneAngle", mAlwaysChangingPhoneAngle);
			*/
			if (mAlwaysChangingPhoneAngle == degrees) {
				return;
			}
			mAlwaysChangingPhoneAngle = degrees;


			Log.d("Phone orientation changed to ", degrees);
			lastDeviceAngle = degrees;
			cameraPreview.removeAllViews();
			mPreview.surfaceDestroyed(mPreview.getHolder());
			mPreview = new CameraPreview(myContext, mCamera);
			cameraPreview.addView(mPreview);

		}
	}
	@Override
	public void onResume() {
		super.onResume();

		mOrientationHelper.enable();
		LinphoneActivity.instance().mOrientationHelper.disable();
		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.DIALER);
			LinphoneActivity.instance().updateDialerFragment(this);
			LinphoneActivity.instance().showStatusBar();
		}

		if (shouldEmptyAddressField) {
			mAddress.setText("");
		} else {
			shouldEmptyAddressField = true;
		}
		if (!hasCamera(myContext)) {
			Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
			toast.show();
		}


		initialize_camera();
		resetLayout(isCallTransferOngoing);
	}
	private boolean hasCamera(Context context) {
		//check if the device has camera
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		//releaseCamera();
		//cameraPreview = null;
//		if (androidVideoWindowImpl != null) {
//			// Prevent linphone from crashing if correspondent hang up while you are rotating
//			androidVideoWindowImpl.release();
//			androidVideoWindowImpl = null;
//		}
	}

	public void resetLayout(boolean callTransfer) {
		isCallTransferOngoing = callTransfer;
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) {
			return;
		}

		if (lc.getCallsNb() > 0) {
			if (isCallTransferOngoing) {
				mCall.setImageResource(R.drawable.transfer_call);
				mCall.setExternalClickListener(transferListener);
			} else {
				mCall.setImageResource(R.drawable.add_call);
				mCall.resetClickListener();
			}
			mAddContact.setEnabled(true);
			mAddContact.setImageResource(R.drawable.cancel);
			mAddContact.setOnClickListener(cancelListener);
		} else {
			mAddContact.setEnabled(true);


			mCall.setImageResource(R.drawable.call_button_new);
			mAddContact.setImageResource(R.drawable.add_contact_new);



			mAddContact.setOnClickListener(addContactListener);
			enableDisableAddContact();
		}
	}

	public void enableDisableAddContact() {
		mAddContact.setEnabled(LinphoneManager.getLc().getCallsNb() > 0 || !mAddress.getText().toString().equals(""));	
	}

	public void displayTextInAddressBar(String numberOrSipAddress) {
		shouldEmptyAddressField = false;
		mAddress.setText(numberOrSipAddress);
	}

	public void newOutgoingCall(String numberOrSipAddress) {
		displayTextInAddressBar(numberOrSipAddress);
		LinphoneManager.getInstance().newOutgoingCall(mAddress);
	}

	public void newOutgoingCall(Intent intent) {
		if (intent != null && intent.getData() != null) {
			String scheme = intent.getData().getScheme();
			if (scheme.startsWith("imto")) {
				mAddress.setText("sip:" + intent.getData().getLastPathSegment());
			} else if (scheme.startsWith("call") || scheme.startsWith("sip")) {
				mAddress.setText(intent.getData().getSchemeSpecificPart());
			} else {
				Uri contactUri = intent.getData();
				String address = ContactsManager.getInstance().queryAddressOrNumber(LinphoneService.instance().getContentResolver(),contactUri);
				if(address != null) {
					mAddress.setText(address);
				} else {
					Log.e("Unknown scheme: ", scheme);
					mAddress.setText(intent.getData().getSchemeSpecificPart());
				}
			}

			mAddress.clearDisplayedName();
			intent.setData(null);

			LinphoneManager.getInstance().newOutgoingCall(mAddress);
		}
	}
}
class SpinnerAdapter extends ArrayAdapter<String> {

	int[] drawables;
	public SpinnerAdapter(Context ctx, int txtViewResourceId,
						  String[] objects, int [] drawable) {
		super(ctx, txtViewResourceId, objects);
		this.drawables = drawable;

	}

	@Override
	public View getDropDownView(int position, View cnvtView, ViewGroup prnt) {
		return getCustomViewSpinner(position, cnvtView, prnt);
	}

	@Override
	public View getView(int pos, View cnvtView, ViewGroup prnt) {
		return getCustomView(pos, cnvtView, prnt);
	}

	public View getCustomView(int position, View convertView,
							  ViewGroup parent) {
		LayoutInflater inflater = LinphoneActivity.instance().getLayoutInflater();
		View mySpinner = inflater.inflate(R.layout.provider_spinner_image_only, parent,
				false);
//		TextView main_text = (TextView) mySpinner.findViewById(R.id.txt);
//		main_text.setText(getItem(position));
		ImageView left_icon = (ImageView) mySpinner.findViewById(R.id.iv);
		left_icon.setImageResource( this.drawables[position]/*R.drawable.provider_logo_sorenson*/ );

		return mySpinner;
	}

	@Override //Disable until General Release
	public boolean isEnabled(int position) { return false; }




	public View getCustomViewSpinner(int position, View convertView,
									 ViewGroup parent) {
		LayoutInflater inflater = LinphoneActivity.instance().getLayoutInflater();
		View mySpinner = inflater.inflate(R.layout.spinner_dropdown_item, parent,
				false);
		TextView main_text = (TextView) mySpinner.findViewById(R.id.txt);
		main_text.setText(getItem(position));
		ImageView left_icon = (ImageView) mySpinner.findViewById(R.id.iv);
		left_icon.setImageResource(this.drawables[position]/*R.drawable.provider_logo_sorenson*/);
		if(!isEnabled(position)){
			mySpinner.setBackgroundColor(Color.DKGRAY);
			left_icon.setColorFilter(Color.GRAY);
			main_text.setTextColor(Color.WHITE);
		}
		return mySpinner;



	}

}
