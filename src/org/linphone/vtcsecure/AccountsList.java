package org.linphone.vtcsecure;



/**
 * @author Patrick Watson
 *
 */

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.linphone.R;

public class AccountsList extends ArrayAdapter<String>{

	private final Activity context;
	private final Integer[] registeredLED;
	private final String[] accountString;
	private final Uri[] providerImage;
	public AccountsList(Activity context, Integer[] registeredLED,
						String[] accountString, Uri[] providerImage) {
		super(context, R.layout.account_list_row, accountString);
		this.context = context;

		this.registeredLED=registeredLED;
		this.accountString = accountString;
		this.providerImage = providerImage;

	}
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView= inflater.inflate(R.layout.account_list_row, null, true);

		ImageView registered_led = (ImageView) rowView.findViewById(R.id.registered_led);
		TextView account_name = (TextView) rowView.findViewById(R.id.account_name);
		ImageView provider_icon = (ImageView) rowView.findViewById(R.id.provider_icon);


		registered_led.setImageResource(registeredLED[position]);
		account_name.setText(accountString[position]);
		provider_icon.setImageURI(providerImage[position]);

		return rowView;
	}
}