package com.vtcsecure.custom;

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

import com.vtcsecure.R;
import com.vtcsecure.setup.SetupActivity;

public class LoginProviderActivity extends Fragment implements OnClickListener {
	
	EditText et_login, et_psw;
	Spinner sp_provider;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = inflater.inflate(R.layout.login_provider, container, false);
		v.findViewById(R.id.btn_login_1).setOnClickListener(this);
		v.findViewById(R.id.btn_login_2).setOnClickListener(this);
		v.findViewById(R.id.btn_login_3).setOnClickListener(this);
		
		
		v.findViewById(R.id.btn_prv_login).setOnClickListener(this);
		
		et_login = (EditText) v.findViewById(R.id.et_prv_user);
		et_psw = (EditText) v.findViewById(R.id.et_prv_pass);
		sp_provider = (Spinner) v.findViewById(R.id.sp_prv);
		
		sp_provider.setAdapter(new SpinnerAdapter(getActivity(), R.layout.spiner_ithem, new String[]{ "asd", "asd2"}));
		
		return v;
	}

	@Override
	public void onClick(View arg0) {
		
		((SetupActivity)getActivity()).onClick(arg0);
	
		
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
