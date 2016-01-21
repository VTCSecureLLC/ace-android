package org.linphone.setup;
/*
GenericLoginFragment.java
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.AsyncProviderLookupOperation;
import org.linphone.LinphoneActivity;
import org.linphone.R;
import org.linphone.core.LinphoneAddress;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class GenericLoginFragment extends Fragment implements OnClickListener, AsyncProviderLookupOperation.ProviderNetworkOperationListener {
	private EditText login, password, domain, port, userid;
	private Spinner transport;
	private ImageView apply;
	View advancedLoginPanel;
	Button advancedLoginPanelToggle;
	boolean isAdvancedLogin = false;
	Spinner sp_provider;
	List<String> transportOptions = new ArrayList<String>();
	private SharedPreferences sharedPreferences;
	AsyncProviderLookupOperation providerLookupOperation;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.login_provider, container, false);
		login = (EditText) view.findViewById(R.id.et_prv_user);
		password = (EditText) view.findViewById(R.id.et_prv_pass);
		password.setImeOptions(EditorInfo.IME_ACTION_DONE);
		domain = (EditText) view.findViewById(R.id.et_prv_domain);
		
			port = (EditText) view.findViewById(R.id.et_prv_port);
			transport = (Spinner) view.findViewById(R.id.spin_prv_transport);

			transportOptions.add("TCP");
			transportOptions.add("TLS");

			try {
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(LinphoneActivity.ctx,
						android.R.layout.simple_spinner_item, transportOptions);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				transport.setAdapter(dataAdapter);
				transport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						providerLookupOperation = new AsyncProviderLookupOperation(GenericLoginFragment.this, getContext());
						providerLookupOperation.execute();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {

					}
				});
			}

			catch(Exception e){
				Log.e("E", "Spinner is not supported by default on this device. Defaulting to TCP transport.");
			}
			userid = (EditText) view.findViewById(R.id.et_prv_userid);

			view.findViewById(R.id.btn_prv_login).setOnClickListener(this);
			sp_provider = (Spinner) view.findViewById(R.id.sp_prv);
			sp_provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					populateRegistrationInfo("provider" + String.valueOf(position) + "domain");
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {

				}
			});
			view.findViewById(R.id.ab_back).setOnClickListener(this);

			advancedLoginPanel = view.findViewById(R.id.advancedLoginPanel);
			advancedLoginPanel.setVisibility(View.GONE);
			advancedLoginPanelToggle = (Button) view.findViewById(R.id.toggleAdvancedLoginPanel);
			advancedLoginPanelToggle.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isAdvancedLogin) {
						advancedLoginPanel.setVisibility(View.VISIBLE);
						((Button) v).setText("-");
						isAdvancedLogin = true;
						password.setImeOptions(EditorInfo.IME_ACTION_NEXT);
					} else {
						advancedLoginPanel.setVisibility(View.GONE);
						((Button) v).setText("+");
						isAdvancedLogin = false;
						password.setImeOptions(EditorInfo.IME_ACTION_DONE);
					}
				}
			});

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

		providerLookupOperation= new AsyncProviderLookupOperation(GenericLoginFragment.this, getContext());
		providerLookupOperation.execute();
		loadProviderDomainsFromCache();
		return view;
	}
	/**
	 * @param query URL to perform service lookup
	 * @param key Key to save record to SharedPreferences as, pass "" or null to not persist
	 */
	//Helper function to lookup an SRV record given a URL String representation
	protected void srvLookup(String query, String key){
		try { //Perform lookup on URL and iterate through all records returned, printing hostname:port to stdout
			Record[] records = new Lookup(query, Type.SRV).run();
			String value;
			if (records != null) {
				for (Record record : records) {
					SRVRecord srv = (SRVRecord) record;
					String hostname = srv.getTarget().toString().replaceFirst("\\.$", "");
					int port = srv.getPort();
					value = hostname + "::" + String.valueOf(port);
					if(key != null && !key.equals("")) {
						sharedPreferences.edit().putString(key, value).apply();
					}
				}
			}
		}catch(TextParseException e){
			e.printStackTrace();
		}
	}
	public List<String> domains = new ArrayList<String>();



	protected void loadProviderDomainsFromCache(){
		//Load cached providers and their domains
		String name = sharedPreferences.getString("provider0", "-1");
		domains = new ArrayList<String>();
		for (int i = 1; !name.equals("-1"); i++) {
			domains.add(name);
			name = sharedPreferences.getString("provider" + String.valueOf(i), "-1");
		}

		setProviderData(domains);
		//Load default provider registration info if cached
		populateRegistrationInfo("provider" + String.valueOf(0) + "domain");
	}

	protected void setProviderData(List<String> data){
		String[] mData = new String[data.size()];
		sp_provider.setAdapter(new SpinnerAdapter(SetupActivity.instance(), R.layout.spiner_ithem,
				mData, new int[]{R.drawable.provider_logo_sorenson,
				R.drawable.provider_logo_zvrs,
				R.drawable.provider_logo_caag,//caag
				R.drawable.provider_logo_purplevrs,
				R.drawable.provider_logo_globalvrs,//global
				R.drawable.provider_logo_convorelay}));
	}

	/**
	 * @param providerDomainKey Key that was used to store registrar in SharedPreferences
	 */
	protected void populateRegistrationInfo(String providerDomainKey){
		String domainString = sharedPreferences.getString(providerDomainKey, "");
		domain.setText(domainString);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.btn_prv_login) {
			if (login.getText() == null || login.length() == 0 || password.getText() == null || password.length() == 0 || domain.getText() == null || domain.length() == 0) {
				Toast.makeText(getActivity(), getString(R.string.first_launch_no_login_password), Toast.LENGTH_LONG).show();
				return;
			}

			//set default transport to tcp
			LinphoneAddress.TransportType transport_type = null;
			try {
				String selectedTransport = transportOptions.get(transport.getSelectedItemPosition());
				if (selectedTransport.toLowerCase().equals("tcp")) {
					transport_type = LinphoneAddress.TransportType.LinphoneTransportTcp;
				} else if (selectedTransport.toLowerCase().equals("tls")) {
					transport_type = LinphoneAddress.TransportType.LinphoneTransportTls;
				} else {
					transport_type = LinphoneAddress.TransportType.LinphoneTransportTcp;
				}
			}

			catch(Exception e){
				Log.e("E", "Transport could not be found, defaulting to TCP");
				transport_type = LinphoneAddress.TransportType.LinphoneTransportTcp;
			}
			SetupActivity.instance().genericLogIn(
					login.getText().toString().replaceAll("\\s", ""),
					password.getText().toString().replaceAll("\\s", ""),
					domain.getText().toString().replaceAll("\\s", ""),
					userid.getText().toString().replaceAll("\\s", ""),
					transport_type,
					port.getText().toString().replaceAll("\\s", ""));
			
		}
		else if(id == R.id.ab_back)
			getActivity().onBackPressed();
	}

	@Override
	public void onProviderLookupFinished(ArrayList<String> mDomains) {
		if(mDomains == null){ return; }
		domains = mDomains;
		if (domains.size() > 0) {
			if (sp_provider != null && sp_provider.getAdapter() != null && sp_provider.getAdapter().getCount() != domains.size()) {
				setProviderData(domains);
			}
			if(sp_provider != null) {
				populateRegistrationInfo("provider" + String.valueOf(sp_provider.getSelectedItemPosition()) + "domain");
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
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View mySpinner = inflater.inflate(R.layout.spiner_ithem, parent,
					false);

			TextView main_text = (TextView) mySpinner.findViewById(R.id.txt);
			String providerName = "";
			if(domains != null && domains.size() > 0) {
				providerName = domains.get(position);
				main_text.setText(providerName);
			}
			ImageView left_icon = (ImageView) mySpinner.findViewById(R.id.iv);
			if(providerName.toLowerCase().contains("sorenson")) {
				left_icon.setImageResource(R.drawable.provider_logo_sorenson);
			}
			else if(providerName.toLowerCase().contains("zvrs")) {
				left_icon.setImageResource(R.drawable.provider_logo_zvrs);
			}
			else if(providerName.toLowerCase().contains("star")) {
				left_icon.setImageResource(R.drawable.provider_logo_caag);
			}
			else if(providerName.toLowerCase().contains("convo")){
				left_icon.setImageResource(R.drawable.provider_logo_convorelay);
			}
			else if(providerName.toLowerCase().contains("global")){
				left_icon.setImageResource(R.drawable.provider_logo_globalvrs);
			}
			else if(providerName.toLowerCase().contains("purple")){
				left_icon.setImageResource(R.drawable.provider_logo_purplevrs);
			}
			else if(providerName.toLowerCase().contains("ace")){
				left_icon.setImageResource(R.drawable.ic_launcher);
			}
			else{
				left_icon.setImageResource(R.drawable.ic_launcher);
			}
			return mySpinner;
		}

		public View getCustomViewSpinner(int position, View convertView,
				ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View mySpinner = inflater.inflate(R.layout.spinner_dropdown_item, parent,
					false);

			TextView main_text = (TextView) mySpinner.findViewById(R.id.txt);
			String providerName = "";
			if(domains != null && domains.size() > 0) {
				try {
					providerName = domains.get(position);
					main_text.setText(providerName);
				}
				catch(IndexOutOfBoundsException e){
					main_text.setText("");
				}
			}
			ImageView left_icon = (ImageView) mySpinner.findViewById(R.id.iv);
			if(providerName.toLowerCase().contains("sorenson")) {
				left_icon.setImageResource(R.drawable.provider_logo_sorenson);
			}
			else if(providerName.toLowerCase().contains("zvrs")) {
				left_icon.setImageResource(R.drawable.provider_logo_zvrs);
			}
			else if(providerName.toLowerCase().contains("star")) {
				left_icon.setImageResource(R.drawable.provider_logo_caag);
			}
			else if(providerName.toLowerCase().contains("convo")){
				left_icon.setImageResource(R.drawable.provider_logo_convorelay);
			}
			else if(providerName.toLowerCase().contains("global")){
				left_icon.setImageResource(R.drawable.provider_logo_globalvrs);
			}
			else if(providerName.toLowerCase().contains("purple")){
				left_icon.setImageResource(R.drawable.provider_logo_purplevrs);
			}
			else if(providerName.toLowerCase().contains("ace")){
				left_icon.setImageResource(R.drawable.ic_launcher);
			}
			return mySpinner;
		}

	}
}
