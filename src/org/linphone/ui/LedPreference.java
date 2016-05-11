package org.linphone.ui;

/*
LedPreference.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.view.View;
import android.widget.ImageView;

import org.linphone.LinphoneActivity;
import org.linphone.LinphonePreferences;
import org.linphone.LinphoneService;
import org.linphone.R;
import org.linphone.setup.SetupActivity;

/**
 * @author Sylvain Berfini
 */
public class LedPreference extends Preference
{
	private int ledDrawable;
    private int accountId;
    private LedPreference pref;
    private Activity activity;
	
	public LedPreference(int accountId, Activity activity) {
        super(activity);
        ledDrawable = R.drawable.led_disconnected;
        this.setWidgetLayoutResource(R.layout.preference_led);
        this.accountId=accountId;
        pref=this;
        this.activity=activity;
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);

        final ImageView imageView = (ImageView) view.findViewById(R.id.led);
        if (imageView != null) {
            imageView.setImageResource(ledDrawable);
        }
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {


                final String enabled_status=LinphonePreferences.instance().isAccountEnabled(accountId)?"Disable":"Enable";
                final String[] single_account_options = {"Logout"};
                String[] multi_account_options = {"Use as default", enabled_status, "Delete this account"};
                final String[] options;

                if(LinphonePreferences.instance().getAccountCount()==1){
                   options=single_account_options;
                }else{
                   options=multi_account_options;
                }

                new AlertDialog.Builder(LinphoneActivity.instance().ctx)
                        .setTitle("Manage Account")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String account_name=LinphonePreferences.instance().getAccountUsername(accountId)+"@"+LinphonePreferences.instance().getAccountDomain(accountId);
                        /* User clicked so do some stuff */
                                if(which==0){//use as default or Logout
                                    if(options==single_account_options){
                                        LinphonePreferences.instance().deleteAccount(accountId);
                                        //deleteDefaultAccount();
                                        Intent intent = new Intent(LinphoneService.instance(), SetupActivity.class);
                                        activity.startActivityForResult(intent, LinphoneActivity.FIRST_LOGIN_ACTIVITY);
                                    }else{
                                        LinphonePreferences.instance().setDefaultAccount(accountId);
                                    }
                                }else if(which==1){//disable
                                    LinphonePreferences.instance().setAccountEnabled(accountId, !LinphonePreferences.instance().isAccountEnabled(accountId));
                                }else if(which==2){//delete
                                    LinphonePreferences.instance().deleteAccount(accountId);
                                }

                                LinphoneActivity.instance().displaySettings();

//                                //Todo refresh preferences screen
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                return false;
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinphoneActivity.instance().displayAccountSettings(accountId);
            }
        });
    }

    public void setLed(int led) {
        ledDrawable = led;
        notifyChanged();
    }
}
