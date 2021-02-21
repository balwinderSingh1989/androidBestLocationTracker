package com.location.bestlocationstrategy

import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Created by Balwinder on 20/Sept/2018.
 */
object LocationUtils {
    private const val TWO_MINUTES = 1000 * 60 * 2
    private const val TAG = "LocationUtils"
    var LastKnownLocaiton: Location? = null

    /**
     * Return the availability of GooglePlayServices
     */
    fun isGooglePlayServicesAvailable(context: Context?): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(context as Activity?, status, 2404).show()
            }
            return false
        }
        return true
    }

    /**
     * GEt best location strategy available .
     *
     * @param ctx
     * @return
     */
    fun getLocationStatergy(ctx: Context?): BaseLocationStrategy? {
        return if (isGooglePlayServicesAvailable(ctx)) {
            //GooglePlayServiceLocationStrategy
            GooglePlayServiceLocationStrategy.getInstance(ctx)
        } else {
            //LocationManagerStrategy
            LocationManagerStrategy.getInstance(ctx!!)
        }
    }

    /**
     * Return the Location Enabled
     */
    fun isLocationProviderEnabled(context: Context): Boolean {
        val locationManager = context
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * @param location
     * @return
     */
    fun isBetterLocation(location: Location): Boolean {
        if (LastKnownLocaiton == null) {
            LastKnownLocaiton = location
            // A new location is always better than no location
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDelta = location.time - LastKnownLocaiton!!.time
        val isSignificantlyNewer = timeDelta > TWO_MINUTES
        val isSignificantlyOlder = timeDelta < -TWO_MINUTES
        val isNewer = timeDelta > 0

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            LastKnownLocaiton = location
            return true
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false
        }

        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (location.accuracy - LastKnownLocaiton!!.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        // Check if the old and new location are from the same provider
        val isFromSameProvider = isSameProvider(location.provider,
                LastKnownLocaiton!!.provider)

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            LastKnownLocaiton = location
            return true
        } else if (isNewer && !isLessAccurate) {
            LastKnownLocaiton = location
            return true
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            LastKnownLocaiton = location
            return true
        }
        return false
    }

    /**
     * Checks whether two providers are the same
     */
    private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
        if (provider1 == null) {
            return provider2 == null
        }
        Log.d(TAG, "Provider " + provider1 + "and " + provider2)
        return provider1 == provider2
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
    fun getLocationMode(context: Context): Int {
        return try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e: SettingNotFoundException) {
            -1
        }
    }
}