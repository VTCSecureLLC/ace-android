package org.linphone;
/*
DialerFragment.java
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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.setup.ApplicationPermissionManager;
import org.linphone.setup.CDNProviders;
import org.linphone.setup.SpinnerAdapter;
import org.linphone.ui.AddressText;
import org.linphone.ui.CallButton;
import org.linphone.ui.EraseButton;
import org.linphone.ui.Numpad;

import java.util.ArrayList;

/**
 * @author Sylvain Berfini
 */
public class DialerFragment extends Fragment implements AsyncProviderLookupOperation.ProviderNetworkOperationListener {

	//public OrientationEventListener mOrientationHelper;
	public Camera mCamera;
	public static Camera.Size optimal_preview_size;

	private static DialerFragment instance;
	private static boolean isCallTransferOngoing = false;
	public CameraPreview mPreview;
	private Context myContext;
	public boolean mVisible;
	private AddressText mAddress;
	public CallButton mCall;
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
	private AsyncProviderLookupOperation providerLookupOperation;
	private boolean isSpinnerOpen = false;
	protected SharedPreferences sharedPreferences;
	protected ArrayList<String> domains;
	private Spinner sipDomainSpinner;
	public boolean isCalled = false;
	Numpad numpad;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		instance = this;
		final View view = inflater.inflate(R.layout.dialer, container, false);

		dialer_content = view.findViewById(R.id.dialerContent);
		isCalled = false;

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

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		// VTCSecure SIP Domain selection 
		sipDomainSpinner= (Spinner)view.findViewById(R.id.sipDomainSpinner);
		sipDomainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(!isCalled){
					isCalled = true;
					return;
				}
				CDNProviders.getInstance().setSelectedProvider(position);
				mAddress.setTag(CDNProviders.getInstance().getSelectedProvider().getDomain());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		final TextView sipDomainTextView = (TextView)view.findViewById(R.id.sipDomainTextView);
		//final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),android.R.layout.simple_spinner_item, domains);
		//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		setProviderData();
		if ( CDNProviders.getInstance().getProvidersCount() == 0 && !AsyncProviderLookupOperation.isAsyncTaskRuning) {
			Log.e("ttt DialFragment AsyncProviderLookupOperation..");
			providerLookupOperation = new AsyncProviderLookupOperation(DialerFragment.this, getContext());
//			providerLookupOperation.execute();
		}
		else if(AsyncProviderLookupOperation.isAsyncTaskRuning && AsyncProviderLookupOperation.getInstance()!=null)
		{
			providerLookupOperation = AsyncProviderLookupOperation.getInstance();
			providerLookupOperation.addListener(this);
		}

		sipDomainTextView.setText("");
		mAddress.setTag(null);
		EraseButton erase = (EraseButton) view.findViewById(R.id.Erase);
		erase.setAddressWidget(mAddress);

		mCall = (CallButton) view.findViewById(R.id.Call);

		//Make text to right of call button clickable.
		TextView call_button_text = (TextView)view.findViewById(R.id.call_button_text);
		call_button_text.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCall.performClick();
				//Event

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

		numpad = (Numpad) view.findViewById(R.id.Dialer);
		if (numpad != null) {
			numpad.setAddressWidget(mAddress);
			numpad.setDTMFSoundEnabled(true);
			numpad.setHapticEnabled(true);
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

		erase.setImageResource(R.drawable.backspace_new);
		mAddContact.setImageResource(R.drawable.add_contact_new);



		view.setBackgroundResource(R.drawable.background_theme_new);

		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myContext = getActivity().getApplication().getBaseContext();
        
       // startOrientationSensor();

		Log.d("dialer oncreat");
	return view;
}


	protected void setProviderData(){
		sipDomainSpinner.setAdapter(new SpinnerAdapter(LinphoneActivity.ctx, R.layout.spiner_ithem,
				new String[]{""}, new int[]{0}, false));
		int selection = CDNProviders.getInstance().getSelectedProviderPosition();
		Log.d("ttt xxx: " + selection);


		sipDomainSpinner.post(new Runnable() {
			@Override
			public void run() {
				sipDomainSpinner.setSelection(CDNProviders.getInstance().getSelectedProviderPosition());

			}
		});
	}

	public void  initialize_camera(){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.ctx);
		String previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
		boolean isPreviewEnabled = prefs.getBoolean(previewIsEnabledKey, true);
		try {
			if(ApplicationPermissionManager.isPermissionGranted(getActivity(), Manifest.permission.CAMERA)&&isPreviewEnabled){
				initialize_camera(dialer_view);
			}else{
				cameraPreview.removeAllViews();
			}
		}catch(Throwable e){
			e.printStackTrace();
		}
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void initialize_camera(View view) {
		cameraPreview = (LinearLayout) view.findViewById(R.id.camera_preview);
		cameraPreview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (dialer_content != null) {
					dialer_content.setVisibility(View.VISIBLE);
				}
				VIEW_INDEX = DialerFragment.instance().SELF_VIEW_INDEX;
			}
		});


		Log.d("mCamera" + mCamera);
		try
		{
			if(cameraPreview.getChildCount() == 1)
			{
				mPreview = (CameraPreview) cameraPreview.getChildAt(0);

				mPreview.refreshCamera();
			}
			else
			{
				mPreview = new CameraPreview(myContext, mCamera);
				cameraPreview.addView(mPreview);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}




	}


    /**
	 * @return null if not ready yet
	 */
	public static DialerFragment instance() { 
		return instance;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPreview != null) {
			mPreview.surfaceDestroyed(null);
		}
	}

	@Override
	public void onProviderLookupFinished() {
		Log.d("ttt onProviderLookupFinished");
		setProviderData();
	}

	@Override
	public void onResume() {
		super.onResume();
		numpad.recheckSystemSettings();
		//mOrientationHelper.enable();
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
		if(AsyncProviderLookupOperation.isAsyncTaskRuning && AsyncProviderLookupOperation.getInstance()!=null)
			AsyncProviderLookupOperation.getInstance().removeListener(this);

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
				//VATRP-2093 Android: Plus image appearing intermediately, on new calls, and during transfer of calls.
				//VATRP-2110 Android: When receiving a call the app shows a big "+" sign for a moment then goes to answer view
				//mCall.setImageResource(R.drawable.add_call);
				mCall.resetClickListener();
			}
			mAddContact.setEnabled(true);
			//VATRP-2114 Android strange back arrow shows when incoming call occurs on dialer screen.
			//mAddContact.setImageResource(R.drawable.cancel);
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

