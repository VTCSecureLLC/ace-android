package org.linphone.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.linphone.R;

/**
 * Created by accontech-samson on 3/8/16.
 */
public class AccountHelper {

    public static boolean hasAccount(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String res =prefs.getString(context.getResources().getString(R.string.pref_user_account), "");
        boolean hasAccount = res.length() > 0 ? true : false;
        return hasAccount;
    }


    public static void setAccount(Context context, String user)
    {
        SharedPreferences.Editor mEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        mEditor.putString(context.getResources().getString(R.string.pref_user_account), user);
        mEditor.commit();
    }

    public static void deleteAccount(Context context)
    {
        setAccount(context, "");
    }

    public static String getAccount(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String res =prefs.getString(context.getResources().getString(R.string.pref_user_account), "");
        return res;
    }



}
