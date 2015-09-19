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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vatrp.R;
/**
 * @author Sylvain Berfini
 */
public class GenericLoginFragment extends Fragment implements OnClickListener {
	private EditText login, password, domain;
	private ImageView apply;
	Spinner sp_provider;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.login_provider, container, false);
		
		login = (EditText) view.findViewById(R.id.et_prv_user);
		password = (EditText) view.findViewById(R.id.et_prv_pass);
		domain = (EditText) view.findViewById(R.id.et_prv_domain);
		view.findViewById(R.id.btn_prv_login).setOnClickListener(this);
		sp_provider = (Spinner) view.findViewById(R.id.sp_prv);
		
		sp_provider.setAdapter(new SpinnerAdapter(getActivity(), R.layout.spiner_ithem, new String[]{ "Provider 1", "Provider 2", "etc"}));
		
		view.findViewById(R.id.ab_back).setOnClickListener(this);
		
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
			
			SetupActivity.instance().genericLogIn(login.getText().toString(), password.getText().toString(), domain.getText().toString());
			
		}
		else if(id == R.id.ab_back)
			getActivity().onBackPressed();
	}
	
	

	
	class SpinnerAdapter extends ArrayAdapter<String> {

		public SpinnerAdapter(Context ctx, int txtViewResourceId,
				String[] objects) {
			super(ctx, txtViewResourceId, objects);
		}

		@Override
		public View getDropDownView(int position, View cnvtView, ViewGroup prnt) {
			return getCustomView(position, cnvtView, prnt);
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
			left_icon.setImageResource(R.drawable.provider_logo_sorenson);

			return mySpinner;
		}
	}
}
