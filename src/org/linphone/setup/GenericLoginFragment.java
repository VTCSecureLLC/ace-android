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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.LegalRelease;
import org.linphone.LinphoneActivity;
import org.linphone.R;
import org.linphone.core.LinphoneAddress;

/**
 * @author Sylvain Berfini
 */
public class GenericLoginFragment extends Fragment implements OnClickListener {
	private EditText login, password, domain, port, transport, userid;;
	private ImageView apply;
	View advancedLoginPanel;
	Button advancedLoginPanelToggle;
	boolean isAdvancedLogin = false;
	Spinner sp_provider;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.login_provider, container, false);
		login = (EditText) view.findViewById(R.id.et_prv_user);
		password = (EditText) view.findViewById(R.id.et_prv_pass);
		password.setImeOptions(EditorInfo.IME_ACTION_DONE);
		domain = (EditText) view.findViewById(R.id.et_prv_domain);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.ctx);
		if(!pref.getBoolean("accepted_legal_release", false)){
			Intent intent = new Intent(LinphoneActivity.ctx, LegalRelease.class);
			LinphoneActivity.ctx.startActivity(intent);
		}
			port = (EditText) view.findViewById(R.id.et_prv_port);
			transport = (EditText) view.findViewById(R.id.et_prv_transport);
			transport.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {

				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {

				}

				@Override
				public void afterTextChanged(Editable s) {
					String transport = s.toString();
					if (transport.toLowerCase().equals("tcp")) {
						//port.setText("5060");
						port.setText(port.getText().toString().replace("5061", "5060"));
					} else if (transport.toLowerCase().equals("tls")) {
						//port.setText("5061");
						port.setText(port.getText().toString().replace("5060", "5061"));
					}
				}
			});
			userid = (EditText) view.findViewById(R.id.et_prv_userid);

			view.findViewById(R.id.btn_prv_login).setOnClickListener(this);
			sp_provider = (Spinner) view.findViewById(R.id.sp_prv);

			sp_provider.setAdapter(new SpinnerAdapter(getActivity(), R.layout.spiner_ithem,
					new String[]{"Sorenson VRS", "ZVRS", "CAAG", "Purple VRS", "Global VRS", "Convo Relay"},
					new int[]{R.drawable.provider_logo_sorenson,
							R.drawable.provider_logo_zvrs,
							R.drawable.provider_logo_caag,//caag
							R.drawable.provider_logo_purplevrs,
							R.drawable.provider_logo_globalvrs,//global
							R.drawable.provider_logo_convorelay}));

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

		return view;
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
			if (transport.getText().toString().toLowerCase().equals("tcp")) {
				transport_type= LinphoneAddress.TransportType.LinphoneTransportTcp;
			} else if (transport.getText().toString().toLowerCase().equals("tls")) {
				transport_type= LinphoneAddress.TransportType.LinphoneTransportTls;
			}
			SetupActivity.instance().genericLogIn(
					login.getText().toString().replaceAll("\\s", ""),
					password.getText().toString().replaceAll("\\s", ""),
					domain.getText().toString().replaceAll("\\s", ""),
					transport_type,
					port.getText().toString().replaceAll("\\s", ""));
			
		}
		else if(id == R.id.ab_back)
			getActivity().onBackPressed();
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
			main_text.setText(getItem(position));
			ImageView left_icon = (ImageView) mySpinner.findViewById(R.id.iv);
			left_icon.setImageResource( this.drawables[position]/*R.drawable.provider_logo_sorenson*/ );

			return mySpinner;
		}

		public View getCustomViewSpinner(int position, View convertView,
				ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View mySpinner = inflater.inflate(R.layout.spinner_dropdown_item, parent,
					false);

			TextView main_text = (TextView) mySpinner.findViewById(R.id.txt);
			main_text.setText(getItem(position));
			ImageView left_icon = (ImageView) mySpinner.findViewById(R.id.iv);
			left_icon.setImageResource( this.drawables[position]/*R.drawable.provider_logo_sorenson*/ );

			return mySpinner;
		}
		
		
	}
}
