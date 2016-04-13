package org.linphone.vtcsecure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import org.linphone.core.Reason;

/**
 * Created by Patrick on 3/2/16.
 */
public class Utils {

    public static String getReasonText(Reason reason) {
        String res = "";
        if (reason == Reason.None) {
            res = "";
        } else if (reason == Reason.NoResponse) {
            res = "The called terminal was not reachable because of technical reasons. (sip: 408)";
        } else if (reason == Reason.BadCredentials) {
            res = "The call did not go through because  of access rights failure. (sip: ?)";
        } else if (reason == Reason.Declined) {
            res = "The call has been declined. (sip: 603)";
        } else if (reason == Reason.NotFound) {
            res = "The number or address could not be found. (sip: 404)";
        } else if (reason == Reason.NotAnswered) {
            res = "No answer.";
        } else if (reason == Reason.Busy) {
            //The number/address you are trying to reach is busy
            res = "The number you are trying to reach is busy (sip: 486)";
        } else if (reason == Reason.Media) {
            res = "The called terminal has no media in common with yours. (sip: 488)";
        } else if (reason == Reason.IOError) {
            res = "Communication error: Bad network connection.";
        } else if (reason == Reason.DoNotDisturb) {
            res = "The call failed becuse of communication problems.";
        } else if (reason == Reason.Unauthorized) {
            res = "The call failed because it requires authorization. (sip: 494)";
        } else if (reason == Reason.NotAcceptable) {
            res = "The call was not accepted of technical reasons by the called terminal. (sip: 406)";
        } else if (reason == Reason.NoMatch) {
            //400 and others ?
            res = "Call failed because called terminal detected and error. (sip: 400)";
        } else if (reason == Reason.MovedPermanently) {
            res = "The called person or organization has changed their number or call address. (sip: 301)";
        } else if (reason == Reason.Gone) {
            //The number/address you are trying to reach is no longer available.
            res = "The number you are trying to reach is no longer available. (sip: 604)";
        } else if (reason == Reason.TemporarilyUnavailable) {
            res = "The person or organization you are trying to reach is not available at this time.  Check the number or address or try again later. (sip: 480)";
        } else if (reason == Reason.AddressIncomplete) {
            res = "The address was incomplete. Please try again. (sip: 484)";
        } else if (reason == Reason.NotImplemented) {
            res = "The call failed because of a service error. (sip: 501)";
        } else if (reason == Reason.BadGateway) {
            res = "Call failed because of error in the communication service. (sip: 502)";
        } else if (reason == Reason.ServerTimeout) {
            res = "The call failed because of a service timeout error. (sip: 504)";
        } else if (reason == Reason.Unknown) {
            res = "The call failed of an unknown error";
        }

        return res;
    }

    public static String removeExtraQuotesFromStringIfPresent(String inputStr){
        String result = inputStr;
        //Check if string has extra quotes
        if(inputStr.startsWith("\"")&&inputStr.endsWith("\"")){
            result=removeQuotesFromStartAndEndOfString(inputStr);
        }
        return result;
    }

    private static String removeQuotesFromStartAndEndOfString(String inputStr) {
        String result = inputStr;
        int firstQuote = inputStr.indexOf('\"');
        int lastQuote = result.lastIndexOf('\"');
        int strLength = inputStr.length();
        if (firstQuote == 0 && lastQuote == strLength - 1) {
            result = result.substring(1, strLength - 1);
        }

        return result;
    }

    private static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Drawable resize(Drawable image, Activity activity, int muliplication_factor) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        int width=muliplication_factor*b.getWidth();
        int height=muliplication_factor*b.getHeight();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, width, height, false);
        return new BitmapDrawable(activity.getResources(), bitmapResized);
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
