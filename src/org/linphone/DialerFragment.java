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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.MediastreamerAndroidContext;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.mediastream.video.capture.AndroidVideoApi5JniWrapper;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
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
	private static DialerFragment instance;
	private static boolean isCallTransferOngoing = false;

	public boolean mVisible;
	private AddressText mAddress;
	private CallButton mCall;
	private ImageView mAddContact;
	private static SurfaceView cameraPreview;
	private AndroidVideoWindowImpl androidVideoWindowImpl;
	private OnClickListener addContactListener, cancelListener, transferListener;
	private boolean shouldEmptyAddressField = true;
	private boolean userInteraction = false;
	private Camera camera;
	String color_theme;
	String background_color_theme;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		instance = this;
		final View view = inflater.inflate(R.layout.dialer, container, false);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.ctx);
		color_theme = prefs.getString(LinphoneActivity.ctx.getResources().getString(R.string.pref_theme_app_color_key), "Tech");
		background_color_theme = prefs.getString(LinphoneActivity.ctx.getResources().getString(R.string.pref_theme_background_color_key), "default");


		mAddress = (AddressText) view.findViewById(R.id.Adress); 
		mAddress.setDialerFragment(this);



		// VTCSecure SIP Domain selection 
		Spinner sipDomainSpinner = (Spinner)view.findViewById(R.id.sipDomainSpinner);
		final TextView sipDomainTextView = (TextView)view.findViewById(R.id.sipDomainTextView);
		sipDomainTextView.setText("");
		String externalDomains = LinphonePreferences.instance().getConfig().getString("vtcsecure", "external_domains", "");
		if (externalDomains.length()>0) {
			externalDomains =","+externalDomains;
			String sipDomains[] = externalDomains.split(",");
			final List<String> sipDomainsList=new ArrayList<String>(Arrays.asList(sipDomains));
			final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),android.R.layout.simple_spinner_item, sipDomainsList);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			sipDomainSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View spinnerView, int position, long id) {  
					if (spinnerView != null) {
						((TextView) spinnerView).setText("");
						((TextView) spinnerView).setTextColor(Color.TRANSPARENT);
					}
					if (position != 0) sipDomainTextView.setText("@"+adapter.getItem(position));
					else sipDomainTextView.setText("");
					mAddress.setTag(sipDomainTextView.getText());
				}  
				public void onNothingSelected(AdapterView<?> arg0) {}  
			});
			sipDomainSpinner.setAdapter(adapter);

		} else {
			sipDomainSpinner.setVisibility(View.GONE);
		}

		EraseButton erase = (EraseButton) view.findViewById(R.id.Erase);
		erase.setAddressWidget(mAddress);


		mCall = (CallButton) view.findViewById(R.id.Call);
		mCall.setAddressWidget(mAddress);
		if (LinphoneActivity.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0) {
			if (isCallTransferOngoing) {
				mCall.setImageResource(R.drawable.transfer_call);
			} else {
				mCall.setImageResource(R.drawable.add_call);
			}
		} else {

			if(color_theme.equals("Red")) {
					mCall.setImageResource(R.drawable.call_red);
			}else if(color_theme.equals("Yellow")) {
					mCall.setImageResource(R.drawable.call_yellow);
			}else if(color_theme.equals("Gray")) {
					mCall.setImageResource(R.drawable.call_gray);
			}else if(color_theme.equals("High Visibility")) {
					mCall.setImageResource(R.drawable.call_hivis);
			}else{
					mCall.setImageResource(R.drawable.call);
			}
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

		if(color_theme.equals("Red")) {
				mAddress.setBackgroundResource(R.drawable.dialer_address_background_theme_red);
				sipDomainSpinner.setBackgroundResource(R.drawable.atbutton_theme_red);
				erase.setImageResource(R.drawable.backspace_red);
				mAddContact.setImageResource(R.drawable.add_contact_red);
		}else if(color_theme.equals("Yellow")) {
				mAddress.setBackgroundResource(R.drawable.dialer_address_background_theme_yellow);
				sipDomainSpinner.setBackgroundResource(R.drawable.atbutton_theme_yellow);
				erase.setImageResource(R.drawable.backspace_yellow);
				mAddContact.setImageResource(R.drawable.add_contact_yellow);
		}else if(color_theme.equals("Gray")) {
				mAddress.setBackgroundResource(R.drawable.dialer_address_background_theme_gray);
				sipDomainSpinner.setBackgroundResource(R.drawable.atbutton_theme_gray);
				erase.setImageResource(R.drawable.backspace_gray);
				mAddContact.setImageResource(R.drawable.add_contact_gray);
		}else if(color_theme.equals("High Visibility")) {
				mAddress.setBackgroundResource(R.drawable.dialer_address_background_theme_hivis);
				sipDomainSpinner.setBackgroundResource(R.drawable.atbutton_theme_hivis);
				erase.setImageResource(R.drawable.backspace_hivis);
				mAddContact.setImageResource(R.drawable.add_contact_hivis);
		}else{
				mAddress.setBackgroundResource(R.drawable.dialer_address_background);
				sipDomainSpinner.setBackgroundResource(R.drawable.atbutton);
				erase.setImageResource(R.drawable.backspace);
				mAddContact.setImageResource(R.drawable.add_contact);
		}

		//set background color independent
		if(background_color_theme.equals("Red")) {
			view.setBackgroundResource(R.drawable.background_theme_red);
		}else if(background_color_theme.equals("Yellow")) {
			view.setBackgroundResource(R.drawable.background_theme_yellow);
		}else if(background_color_theme.equals("Gray")) {
			view.setBackgroundResource(R.drawable.background_theme_gray);
		}else if(background_color_theme.equals("High Visibility")) {
			view.setBackgroundResource(R.drawable.background_theme_hivis);
		}else{
			view.setBackgroundResource(R.drawable.background);
		}

		String previewIsEnabledKey = LinphoneManager.getInstance().getContext().getString(R.string.pref_av_show_preview_key);
		boolean isPreviewEnabled = prefs.getBoolean(previewIsEnabledKey, true);
		if(!LinphoneActivity.instance().isTablet()){
			isPreviewEnabled = false;
		}
		if(isPreviewEnabled) {
			cameraPreview = (SurfaceView) view.findViewById(R.id.dialerCameraPreview);
			if (cameraPreview != null) {
				cameraPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Warning useless because value is ignored and automatically set by new APIs.
				LinphoneManager.getLc().enableVideo(true, true);
				LinphoneManager.getLc().setPreviewWindow(cameraPreview);

					androidVideoWindowImpl = new AndroidVideoWindowImpl(cameraPreview, cameraPreview, new AndroidVideoWindowImpl.VideoWindowListener() {
						@Override
						public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
							LinphoneManager.getLc().setVideoWindow(vw);
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
							try {
								LinphoneManager.getLc().setVideoDevice(LinphoneManager.getLc().getVideoDevice());
								camera = Camera.open(1);
								cameraPreview = surface;
								cameraPreview = (SurfaceView)view.findViewById(R.id.dialerCameraPreview);
								LinphoneManager.getLc().setPreviewWindow(cameraPreview);
								camera.setPreviewDisplay(cameraPreview.getHolder());
								camera.startPreview();
							}
							catch (Exception exc) {
								exc.printStackTrace();
							}


						}

						@Override
						public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {
							// Remove references kept in jni code and restart camera
							LinphoneManager.getLc().setPreviewWindow(null);
						}
					});
				}
			}
		return view;
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
		if (androidVideoWindowImpl != null) {
			synchronized (androidVideoWindowImpl) {
				/*
				 * this call will destroy native opengl renderer which is used by
				 * androidVideoWindowImpl
				 */
				LinphoneManager.getLc().setVideoWindow(null);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
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

		if (androidVideoWindowImpl != null) {
			synchronized (androidVideoWindowImpl) {
				LinphoneManager.getLc().setVideoWindow(androidVideoWindowImpl);
				//LinphoneManager.getLc().setPreviewWindow(cameraPreview);
			}
		}
		resetLayout(isCallTransferOngoing);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		cameraPreview = null;
		if (androidVideoWindowImpl != null) {
			// Prevent linphone from crashing if correspondent hang up while you are rotating
			androidVideoWindowImpl.release();
			androidVideoWindowImpl = null;
		}
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

			if(color_theme.equals("Red")) {
					mCall.setImageResource(R.drawable.call_red);
					mAddContact.setImageResource(R.drawable.add_contact_red);
			}else if(color_theme.equals("Yellow")) {
					mCall.setImageResource(R.drawable.call_yellow);
					mAddContact.setImageResource(R.drawable.add_contact_yellow);
			}else if(color_theme.equals("Gray")) {
					mCall.setImageResource(R.drawable.call_gray);
					mAddContact.setImageResource(R.drawable.add_contact_gray);
			}else if(color_theme.equals("High Visibility")) {
					mCall.setImageResource(R.drawable.call_hivis);
					mAddContact.setImageResource(R.drawable.add_contact_hivis);
			}else{
					mCall.setImageResource(R.drawable.call);
					mAddContact.setImageResource(R.drawable.add_contact);

			}

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
