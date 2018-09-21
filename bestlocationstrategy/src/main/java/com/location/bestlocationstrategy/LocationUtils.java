package com.location.bestlocationstrategy;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import static android.content.Context.LOCATION_SERVICE;

/**
* Created by Balwinder on 20/Sept/2018.
 */

public class LocationUtils {

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final String TAG = "LocationUtils";
    public static Location LastKnownLocaiton;


    /**
     * Return the availability of GooglePlayServices
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog((Activity) context, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    /**
     * GEt best location stratergy available .
     *
     * @param ctx
     * @return
     */
    public static BaseLocationStrategy getLocationStatergy(Context ctx) {
        BaseLocationStrategy baseLocationStrategy;
        if (LocationUtils.isGooglePlayServicesAvailable(ctx)) {
            //GooglePlayServiceLocationStrategy
            baseLocationStrategy = GooglePlayServiceLocationStrategy.getInstance(ctx);
        } else {
            //LocationManagerStrategy
            baseLocationStrategy = LocationManagerStrategy.getInstance(ctx);
        }
        return baseLocationStrategy;
    }

    /**
     * Return the Location Enabled
     */
    public static boolean isLocationProviderEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * @param location
     * @return
     */
    public static boolean isBetterLocation(Location location) {
        if (LastKnownLocaiton == null) {
            LastKnownLocaiton = location;
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - LastKnownLocaiton.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            LastKnownLocaiton = location;
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - LastKnownLocaiton.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                LastKnownLocaiton.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            LastKnownLocaiton = location;
            return true;
        } else if (isNewer && !isLessAccurate) {
            LastKnownLocaiton = location;
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            LastKnownLocaiton = location;
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        Log.d(TAG, "Provider " + provider1 + "and " + provider2);
        return provider1.equals(provider2);
    }

    /**
     * 0 = LOCATION_MODE_OFF
     * 1 = LOCATION_MODE_SENSORS_ONLY
     * 2 = LOCATION_MODE_BATTERY_SAVING
     * 3 = LOCATION_MODE_HIGH_ACCURACY
     *
     * @param context
     * @return
     */
    public static int getLocationMode(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }


}