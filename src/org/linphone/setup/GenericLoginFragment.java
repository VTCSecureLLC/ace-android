package org.linphone.setup;
/*
GenericLoginFragment.java
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import org.linphone.AsyncProviderLookupOperation;
import org.linphone.LinphoneActivity;
import org.linphone.LinphonePreferences;
import org.linphone.R;
import org.linphone.core.LinphoneAddress;
import org.linphone.mediastream.Log;
import org.linphone.vtcsecure.Utils;
import org.linphone.vtcsecure.g;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class GenericLoginFragment extends Fragment implements OnClickListener, AsyncProviderLookupOperation.ProviderNetworkOperationListener {
	private EditText login;
	private EditText password;
	private EditText domain;
	public EditText port;
	private EditText userid;
	public Spinner transport;
	private ImageView apply;
	View advancedLoginPanel;
	Button advancedLoginPanelToggle;
	boolean isAdvancedLogin = false;
	Spinner sp_provider;
	List<String> transportOptions = new ArrayList<String>();
	AsyncProviderLookupOperation providerLookupOperation;

	public Button login_button;
	private static GenericLoginFragment instance;
	public GenericLoginFragment()
	{
		instance = this;
	}

	public static final GenericLoginFragment instance() {
		if (instance != null)
			return instance;
		throw new RuntimeException("GenericLoginFragment not instantiated yet");
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.login_provider, container, false);
		login_button=(Button)view.findViewById(R.id.btn_prv_login);
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
//					providerLookupOperation = new AsyncProviderLookupOperation(GenericLoginFragment.this, getContext());
//					providerLookupOperation.execute();

					//Toggle port depending on transport selection
					int n= LinphonePreferences.instance().getDefaultAccountIndex();
					if(position==0&&port.getText().toString().contains("5061")){//tcp
						port.setText(port.getText().toString().replace("5061", "5060"));
						//LinphonePreferences.instance().setAccountProxy(n, LinphonePreferences.instance().getAccountProxy(n).replace("5061", "5060"));
					}else if(position==1&&port.getText().toString().contains("5060")){//tls
						port.setText(port.getText().toString().replace("5060", "5061"));
						//LinphonePreferences.instance().setAccountProxy(n, LinphonePreferences.instance().getAccountProxy(n).replace("5060", "5061"));
					}
				}
				@Override
				public void onNothingSelected(AdapterView<?> parent) {

				}
			});
		}

		catch(Exception e){
			Log.e("Spinner is not supported by default on this device. Defaulting to TCP transport.");
		}
		userid = (EditText) view.findViewById(R.id.et_prv_userid);

		view.findViewById(R.id.btn_prv_login).setOnClickListener(this);
		sp_provider = (Spinner) view.findViewById(R.id.sp_prv);
		sp_provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				CDNProviders.getInstance().setSelectedProvider(position);
				if (CDNProviders.getInstance().getSelectedProvider() != null) {
					domain.setText(CDNProviders.getInstance().getSelectedProvider().getDomain());
					port.setText(String.valueOf(CDNProviders.getInstance().getSelectedProvider().getPort()));
				}
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

		setProviderData();

		if(sp_provider.getSelectedItemPosition()< CDNProviders.getInstance().getProvidersCount())
		{
			domain.setText(CDNProviders.getInstance().getProvider(sp_provider.getSelectedItemPosition()).getDomain());
		}


		if (CDNProviders.getInstance().getProvidersCount() == 0 && !AsyncProviderLookupOperation.isAsyncTaskRuning) {
			org.linphone.mediastream.Log.e("ttt GenericLoginFragment AsyncProviderLookupOperation..");
			providerLookupOperation = new AsyncProviderLookupOperation(GenericLoginFragment.this, getContext());
			providerLookupOperation.execute();
		} else if (AsyncProviderLookupOperation.isAsyncTaskRuning && AsyncProviderLookupOperation.getInstance() != null) {
			providerLookupOperation = AsyncProviderLookupOperation.getInstance();
			providerLookupOperation.addListener(this);
		}

		//Screen Hit
//		Log.i(Log.TAG, "Setting screen name: LoginScreen");
		g.analytics_tracker.setScreenName("LoginScreen");




		return view;
	}
//	/**
//	 * @param query URL to perform service lookup
//	 * @param key Key to save record to SharedPreferences as, pass "" or null to not persist
//	 */
	//Helper function to lookup an SRV record given a URL String representation
//	protected void srvLookup(String query, String key){
//		try { //Perform lookup on URL and iterate through all records returned, printing hostname:port to stdout
//			Record[] records = new Lookup(query, Type.SRV).run();
//			String value;
//			if (records != null) {
//				for (Record record : records) {
//					SRVRecord srv = (SRVRecord) record;
//					String hostname = srv.getTarget().toString().replaceFirst("\\.$", "");
//					int port = srv.getPort();
//					value = hostname + "::" + String.valueOf(port);
//					if(key != null && !key.equals("")) {
//						sharedPreferences.edit().putString(key, value).apply();
//					}
//				}
//			}
//		}catch(TextParseException e){
//			e.printStackTrace();
//		}
//	}

	protected void setProviderData(){
		sp_provider.setAdapter(new SpinnerAdapter(SetupActivity.instance(), R.layout.spiner_ithem,
				new String[]{""}, new int[]{0}, true));
		sp_provider.setSelection(CDNProviders.getInstance().getSelectedProviderPosition());
		//set text
	}

//	/**
//	 * @param providerDomainKey Key that was used to store registrar in SharedPreferences
//	 */
//	protected void populateRegistrationInfo(String providerDomainKey){
//		String domainString = sharedPreferences.getString(providerDomainKey, "");
//		domain.setText(domainString);
//	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.btn_prv_login) {
			int NO_WIFI_REFRESH=-1;
			if (!Utils.check_network_status(getActivity(), NO_WIFI_REFRESH)) {//dont refresh on return when login button pressed and there wasn't wifi.
				//network isn't available available, dialog shown by check above.
			} else {
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
				} catch (Exception e) {
					Log.e("E", "Transport could not be found, defaulting to TCP");
					transport_type = LinphoneAddress.TransportType.LinphoneTransportTcp;
				}
//			CDNProviders.getInstance().setSelectedProvider(sp_provider.getSelectedItemPosition());
				SetupActivity.instance().genericLogIn(
						login.getText().toString().replaceAll("\\s", ""),
						password.getText().toString().replaceAll("\\s", ""),
						domain.getText().toString().replaceAll("\\s", ""),
						userid.getText().toString().replaceAll("\\s", ""),
						transport_type,
						port.getText().toString().replaceAll("\\s", ""));

			}

			//Event
			g.analytics_tracker.send(LinphoneActivity.instance().getApplicationContext(),"Action","Login Button Pressed",null,null);

		}else if (id == R.id.ab_back){
				getActivity().onBackPressed();
			}

	}

	@Override
	public void onProviderLookupFinished() {
		setProviderData();
		Log.d("onProviderLookupFinished");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(AsyncProviderLookupOperation.getInstance()!=null)
		{
			AsyncProviderLookupOperation.getInstance().removeListener(this);
		}
	}
}