package org.linphone.vtcsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

/**
 * Created by Patrick on 3/2/16.
 */
public class Utils {

    private static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean check_network_status(final Activity activity, final int ACTIVITY_RESULT_INT) {
       if (!isNetworkAvailable(activity)) {
            String message = "Network not reachable, please confirm your device is connected to the internet.";
            new AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setTitle("Connection Error")
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("Turn on WIFI", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(ACTIVITY_RESULT_INT!=-1) {//If you don't want to refresh app after wifi activity.
                                activity.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), ACTIVITY_RESULT_INT);
                            }else{
                                activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            }
                            dialog.cancel();
                        }
                    })
                    .show();
            return false;
        }else{
            return true;
        }
    }

}
