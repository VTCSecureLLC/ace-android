package org.linphone.setup;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;

import org.linphone.LinphoneManager;

/**
 * Created by Vardan on 12/21/2015.
 */
public class ApplicationPermissionManager {


    public static boolean askPermissionifnotGranted(Activity context, String permission, int request_code)
    {
        boolean permission_granted = isPermissionGranted(context, permission);
        if(!permission_granted)
        {
            askPermission(context, permission, request_code);
        }
        return  permission_granted;
    }
    public static void askPermission(Activity context, String permission, int request_code)
    {

        if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                permission)) {
            ActivityCompat.requestPermissions(context,
                    new String[]{permission},
                    request_code);
        } else {

            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(context, new String[]{permission},
                    request_code);
        }

    }
    public static boolean isPermissionGranted(Context context, String permission)
    {


        int permissionCheck = PermissionChecker.checkSelfPermission(context,
                permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED  ;
    }



}
