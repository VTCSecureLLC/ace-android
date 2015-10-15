package com.vtcsecure.ace;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.vtcsecure.R;

import java.util.Calendar;

public class LinphoneLocationManager implements LocationListener {
    LocationManager mLocationManager;
    private static LinphoneLocationManager instance = null;
    private String locationDenied,locationNotFound;
    
    Location userLocation = null;
    
    public synchronized static LinphoneLocationManager instance(Activity a) {
    	if (instance == null) {
    		instance = new LinphoneLocationManager();
    		instance.mLocationManager = (LocationManager) a.getSystemService(Context.LOCATION_SERVICE);
    		instance.locationDenied = a.getString(R.string.gpsdenied);
    		instance.locationNotFound = a.getString(R.string.gpsnolocation);
    	}
    	return instance;
    }

    public void updateLocation() {

        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 2 * 60 * 1000) {
        	userLocation = location;
        }
        else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    public void onLocationChanged(Location location) {
        if (location != null) {
        	userLocation = location;
            mLocationManager.removeUpdates(this);
        }
    }
    
    public Boolean isLocationProviderEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }	
    
    
    public String userLocation() {
    	if (!isLocationProviderEnabled()) return locationDenied;
    	else if (userLocation == null || (userLocation.getLatitude()==0 && userLocation.getLongitude()==0)) return locationNotFound;
    	else return userLocation.getLatitude()+", "+userLocation.getLongitude();
    }

    public void onProviderDisabled(String arg0) {}
    public void onProviderEnabled(String arg0) {}
	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}

}