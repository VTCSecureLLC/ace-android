/*
CallButton.java
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
package org.linphone.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.LinphonePreferences;
import org.linphone.R;
import org.linphone.SettingsFragment;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.setup.CDNProviders;
import org.linphone.vtcsecure.AccountsList;
import org.linphone.vtcsecure.Utils;
import org.linphone.vtcsecure.g;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author Guillaume Beraudo
 */
public class CallButton extends ImageView implements OnClickListener, AddressAware {

	private AddressText mAddress;
	int original_default_account_index;
	public void setAddressWidget(AddressText a) { mAddress = a; }

	public void setExternalClickListener(OnClickListener e) { setOnClickListener(e); }
	public void resetClickListener() { setOnClickListener(this); }

	public CallButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnClickListener(this);
	}

	public void onClick(View v) {
		if (getContext().getResources().getBoolean(R.bool.call_last_log_if_adress_is_empty)&&mAddress.getText().length() == 0) {
			populateAddressBarWithLastDialedNumber();
		}else {
			final SharedPreferences non_linphone_prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneManager.getInstance().getContext());
			Boolean dial_out_using_default_account = non_linphone_prefs.getBoolean(getResources().getString(R.string.dial_out_using_default_account_key), true);
			if (dial_out_using_default_account == true) {
				performOutgoingCall();
				//g.analytics_tracker.send(LinphoneActivity.instance().getApplicationContext(),"Action","Call Attempt Made","Call to provider "+mAddress.getTag()+" attempted",null);
			} else {
				show_account_selector_only_if_multiple_registrations_to_selected_outbound_provider();
			}
		}
	}
	
	protected void onWrongDestinationAddress() {
		Toast.makeText(getContext()
				,String.format(getResources().getString(R.string.warning_wrong_destination_address),mAddress.getText().toString())
				,Toast.LENGTH_LONG).show();
	}

	public void populateAddressBarWithLastDialedNumber(){
		LinphoneCallLog[] logs = LinphoneManager.getLc().getCallLogs();
		LinphoneCallLog log = null;
		for (LinphoneCallLog l : logs) {
			if (l.getDirection() == CallDirection.Outgoing) {
				log = l;
				break;
			}
		}
		if (log == null) {
			return;
		}

		LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
		if (lpc != null && log.getTo().getDomain().equals(lpc.getDomain())) {
			mAddress.setText(log.getTo().getUserName());
		} else {
			mAddress.setText(log.getTo().asStringUriOnly());
		}
		mAddress.setSelection(mAddress.getText().toString().length());
		mAddress.setDisplayedName(log.getTo().getDisplayName());
	}

	public void performOutgoingCall(){

		try {
			if (!LinphoneManager.getInstance().acceptCallIfIncomingPending()) {
				//Parse SIP address url
				if (mAddress.getText().length() > 0) {
					//tag is the provider domain uri selected in dialerfragment, oldAddr is the current string to be dialed
					//If tag is not equal to null, a different provider has been selected
					if (mAddress.getTag() != null) {
						String oldAddr = mAddress.getText().toString();
						String name = "";
						//Check if oldAddr already contains an @ domain, if so, strip it then append the new domain
						if (oldAddr.length() > 1) {
							int domainStart = oldAddr.indexOf("@", 0);
							if (domainStart == -1) {
								domainStart = oldAddr.length();
							}
							//username with @domain stripped
							name = oldAddr.substring(0, domainStart);
						}
						//Combine username with new address to get the proper SIP uri
						String fullAddr = name + "@" + mAddress.getTag();
						mAddress.setText(fullAddr);
						mAddress.setDisplayedName(mAddress.getText().toString());
						mAddress.setTag(null);

						LinphoneManager.getInstance().newOutgoingCall(mAddress);

					}else {
						LinphoneManager.getInstance().newOutgoingCall(mAddress);
					}
				}
			}
		} catch (LinphoneCoreException e) {
			LinphoneManager.getInstance().terminateCall();
			onWrongDestinationAddress();
		};
	}
	public void show_account_selector(){
		// Get already configured extra accounts
		original_default_account_index= LinphonePreferences.instance().getDefaultAccountIndex();
		int nbAccounts = LinphonePreferences.instance().getAccountCount();

		Integer[] registeredLED=new Integer[nbAccounts];
		String[] accountString=new String[nbAccounts];
		Uri[] providerImage=new Uri[nbAccounts];

		for (int i = 0; i < nbAccounts; i++) {
			final int accountId = i;

			String username = LinphonePreferences.instance().getAccountUsername(accountId);
			String domain = LinphonePreferences.instance().getAccountDomain(accountId);

			if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
				for (LinphoneProxyConfig lpc : LinphoneManager.getLc().getProxyConfigList()) {
					LinphoneAddress addr = null;
					try {
						addr = LinphoneCoreFactory.instance().createLinphoneAddress(lpc.getIdentity());
					} catch (LinphoneCoreException e) {
						registeredLED[i]=R.drawable.led_disconnected;
						return;
					}
					if (addr.getUserName().equals(username) && addr.getDomain().equals(domain)) {
						if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationOk) {
							registeredLED[i]=R.drawable.led_connected;
						} else if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationFailed) {
							registeredLED[i]=R.drawable.led_error;
						} else if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationProgress) {
							registeredLED[i]=R.drawable.led_inprogress;
						} else {
							registeredLED[i]=R.drawable.led_disconnected;
						}
						break;
					}
				}
			}


			accountString[i]=username + "@" + domain;
			providerImage[i]= g.domain_image_hash.get(domain);
		}

		final AccountsList accountsList=new AccountsList(LinphoneActivity.instance(), registeredLED, accountString, providerImage);
		new AlertDialog.Builder(LinphoneActivity.instance().ctx)
				.setTitle("Which account would you like to use to place this call?")
				.setSingleChoiceItems(accountsList, original_default_account_index, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						//change default account to force call through the selected account. Set default account back after call is started.
						LinphonePreferences.instance().setDefaultAccount(whichButton);
						performOutgoingCall();
						LinphonePreferences.instance().setDefaultAccount(original_default_account_index);
						dialog.dismiss();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
                        /* User clicked No so do some stuff */
					}
				})
				.create().show();
	}



	public void show_account_selector_only_if_multiple_registrations_to_selected_outbound_provider(){
		// Get already configured extra accounts
		original_default_account_index= LinphonePreferences.instance().getDefaultAccountIndex();
		int nbAccounts = LinphonePreferences.instance().getAccountCount();

		ArrayList<Integer> accounts_registered_to_selected_outbound_provider=new ArrayList();
		//Get only registered accounts with provider
		for(int y=0; y<nbAccounts; y++){
			String domain=LinphonePreferences.instance().getAccountDomain(y);
			if(domain.equals(CDNProviders.getInstance().getSelectedProvider().getDomain())){
				accounts_registered_to_selected_outbound_provider.add(y);
			}
		}

		Integer[] registeredLED=new Integer[accounts_registered_to_selected_outbound_provider.size()];
		String[] accountString=new String[accounts_registered_to_selected_outbound_provider.size()];
		Uri[] providerImage=new Uri[accounts_registered_to_selected_outbound_provider.size()];

		int i=0;
		for (int index:accounts_registered_to_selected_outbound_provider) {

			String username = LinphonePreferences.instance().getAccountUsername(index);
			String domain = LinphonePreferences.instance().getAccountDomain(index);

			if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
				for (LinphoneProxyConfig lpc : LinphoneManager.getLc().getProxyConfigList()) {
					LinphoneAddress addr = null;
					try {
						addr = LinphoneCoreFactory.instance().createLinphoneAddress(lpc.getIdentity());
					} catch (LinphoneCoreException e) {
						registeredLED[i]=R.drawable.led_disconnected;
						return;
					}
					if (addr.getUserName().equals(username) && addr.getDomain().equals(domain)) {
						if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationOk) {
							registeredLED[i]=R.drawable.led_connected;
						} else if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationFailed) {
							registeredLED[i]=R.drawable.led_error;
						} else if (lpc.getState() == LinphoneCore.RegistrationState.RegistrationProgress) {
							registeredLED[i]=R.drawable.led_inprogress;
						} else {
							registeredLED[i]=R.drawable.led_disconnected;
						}
						break;
					}
				}
			}


			accountString[i]=username + "@" + domain;
			providerImage[i]= g.domain_image_hash.get(domain);

			i++;
		}

		final AccountsList accountsList=new AccountsList(LinphoneActivity.instance(), registeredLED, accountString, providerImage);

		if(accounts_registered_to_selected_outbound_provider.size()>1) {
			new AlertDialog.Builder(LinphoneActivity.instance().ctx)
					.setTitle("Which account would you like to use to place this call?")
					.setSingleChoiceItems(accountsList, original_default_account_index, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							//change default account to force call through the selected account. Set default account back after call is started.
							LinphonePreferences.instance().setDefaultAccount(whichButton);
							performOutgoingCall();
							LinphonePreferences.instance().setDefaultAccount(original_default_account_index);
							dialog.dismiss();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
                        /* User clicked No so do some stuff */
						}
					})
					.create().show();
		}else if(accounts_registered_to_selected_outbound_provider.size()==1){
			LinphonePreferences.instance().setDefaultAccount(CDNProviders.getInstance().getSelectedProviderPosition());
			performOutgoingCall();
			LinphonePreferences.instance().setDefaultAccount(original_default_account_index);
		}else if(accounts_registered_to_selected_outbound_provider.size()==0){
			Drawable selected_provider_drawable;
			try {
				InputStream inputStream = LinphoneActivity.instance().ctx.getContentResolver().openInputStream(g.domain_image_hash.get(CDNProviders.getInstance().getSelectedProvider().getDomain()));
				selected_provider_drawable = Drawable.createFromStream(inputStream, g.domain_image_hash.get(CDNProviders.getInstance().getSelectedProvider().getDomain()).toString() );
			} catch (FileNotFoundException e) {
				selected_provider_drawable = getResources().getDrawable(R.drawable.provider_logo_zvrs);
			}
			selected_provider_drawable=Utils.resize(selected_provider_drawable, LinphoneActivity.instance(), 3);
			new AlertDialog.Builder(LinphoneActivity.instance().ctx)
					.setTitle("No registration found!")
					.setIcon(selected_provider_drawable)
					.setMessage("You do not have a registered account with this provider. Please select a different Provider. \n\n\t\t\t\t or \n\n" +
							"If you have an account with this provider please add it using 'Account Setup Assistant'. " +
							"If you do not have an account for this provider please contact the provider" +
							"to have one created.")
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.dismiss();
                        /* User clicked No so do some stuff */
						}
					})
					.setPositiveButton("Go to Account Setup Assistant", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SettingsFragment.showNewAccountDialog(LinphoneActivity.instance());
							dialog.dismiss();
                        /* User clicked No so do some stuff */
						}
					})
					.create().show();
		}
	}
}
