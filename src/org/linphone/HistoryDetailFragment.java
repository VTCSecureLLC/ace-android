package org.linphone;
/*
HistoryDetailFragment.java
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.ui.AvatarWithShadow;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import org.linphone.R;

/**
 * @author Sylvain Berfini
 */
public class HistoryDetailFragment extends Fragment implements OnClickListener {
	private ImageView addToContacts;
	private View view;
	private LayoutInflater inflater;
	private AvatarWithShadow contactPicture;
	private TextView contactName, contactAddress, callDirection, time, date;
	private String sipUri, displayName, pictureUri;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		this.inflater = inflater;
		sipUri = getArguments().getString("SipUri");
		displayName = getArguments().getString("DisplayName");
		pictureUri = getArguments().getString("PictureUri");
		String status = getArguments().getString("CallStatus");
		String callTime = getArguments().getString("CallTime");
		String callDate = getArguments().getString("CallDate");
		
		view = inflater.inflate(R.layout.history_detail, container, false);
		

		
		contactPicture = (AvatarWithShadow) view.findViewById(R.id.contactPicture);
		
		contactName = (TextView) view.findViewById(R.id.contactName);
		if (displayName == null && getResources().getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(sipUri)) {
			displayName = LinphoneUtils.getUsernameFromAddress(sipUri);
		}
		
		contactAddress = (TextView) view.findViewById(R.id.contactAddress);
		
		callDirection = (TextView) view.findViewById(R.id.callDirection);
		
		time = (TextView) view.findViewById(R.id.time);
		date = (TextView) view.findViewById(R.id.date);
		
		displayHistory(status, callTime, callDate);
		displayContact(inflater, view);
		return view;
	}
	
	private void displayHistory(String status, String callTime, String callDate) {
		contactName.setText(displayName == null ? sipUri : displayName);
		if (getResources().getBoolean(R.bool.never_display_sip_addresses)) {
			contactAddress.setText(LinphoneUtils.getUsernameFromAddress(sipUri));
		} else {
			contactAddress.setText(sipUri);
		}
		
		if (status.equals("Missed")) {
			callDirection.setText(getString(R.string.call_state_missed));
		} else if (status.equals("Incoming")) {
			callDirection.setText(getString(R.string.call_state_incoming));
		} else if (status.equals("Outgoing")) {
			callDirection.setText(getString(R.string.call_state_outgoing));
		} else {
			callDirection.setText(status);
		}
		
		time.setText(callTime == null ? "" : callTime);
		date.setText(timestampToHumanDate(callDate));

		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
			Contact contact = ContactsManager.getInstance().findContactWithAddress(getActivity().getContentResolver(), lAddress);
			if (contact != null) {
				LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture.getView(), contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
			} else {
				LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture.getView(),null, null ,R.drawable.unknown_small);
			}
		} catch (LinphoneCoreException e) {
			e.printStackTrace();
		}
	
	}
	
	public void changeDisplayedHistory(String sipUri, String displayName, String pictureUri, String status, String callTime, String callDate) {		
		if (displayName == null && getResources().getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(sipUri)) {
			displayName = LinphoneUtils.getUsernameFromAddress(sipUri);
		}

		this.sipUri = sipUri;
		this.displayName = displayName;
		this.pictureUri = pictureUri;
		displayHistory(status, callTime, callDate);
		displayContact(inflater, view);

	}
	@SuppressLint("InflateParams")
	private void displayContact(LayoutInflater inflater, View view) {
		//AvatarWithShadow contactPicture = (AvatarWithShadow) view.findViewById(R.id.contactPicture);
		//int contactID = Integer.parseInt(contact.getID());
		//String rawContactId = ContactsManager.getInstance().findRawContactID(getActivity().getContentResolver(), String.valueOf(contactID));

		//contactPicture.setImageResource(R.drawable.unknown_small);

		TextView contactName = (TextView) view.findViewById(R.id.contactName);
		contactName.setText(displayName);


		TableLayout controls = (TableLayout) view.findViewById(R.id.controls);
		controls.removeAllViews();

			View v = inflater.inflate(R.layout.contact_control_row, null);

			String displayednumberOrAddress = sipUri;

			TextView tv = (TextView) v.findViewById(R.id.numeroOrAddress);
			tv.setText(displayednumberOrAddress);
			tv.setSelected(true);


			v.findViewById(R.id.dial).setOnClickListener(this);
			v.findViewById(R.id.dial).setTag(displayednumberOrAddress);


			v.findViewById(R.id.start_chat).setOnClickListener(this);
			LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();

			addToContacts = (ImageView) v.findViewById(R.id.addFriend);
			addToContacts.setOnClickListener(this);
			addToContacts.setVisibility(View.VISIBLE);

		controls.addView(v);

	}
	@Override
	public void onResume() {
		super.onResume();
		
		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.HISTORY_DETAIL);
			
			if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
				LinphoneActivity.instance().hideStatusBar();
			}
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		if (id == R.id.dial) {
			LinphoneActivity.instance().setAddresGoToDialerAndCall(sipUri, displayName, pictureUri == null ? null : Uri.parse(pictureUri));
		} else if (id == R.id.start_chat) {
			LinphoneActivity.instance().displayChat(sipUri);
		} else if (id ==R.id.addFriend) {
			String uriToAdd = sipUri;
			if (getResources().getBoolean(R.bool.never_display_sip_addresses)) {
				uriToAdd = LinphoneUtils.getUsernameFromAddress(sipUri);
			}
			LinphoneActivity.instance().displayContactsForEdition(uriToAdd);
		}
	}
	
	@SuppressLint("SimpleDateFormat")
	private String timestampToHumanDate(String timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(Long.parseLong(timestamp));
		
		SimpleDateFormat dateFormat;
		dateFormat = new SimpleDateFormat(getResources().getString(R.string.history_detail_date_format));
		return dateFormat.format(cal.getTime());
	}
}
