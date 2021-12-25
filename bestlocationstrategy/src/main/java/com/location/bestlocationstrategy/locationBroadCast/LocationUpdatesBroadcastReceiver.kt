package com.location.bestlocationstrategy.locationBroadCast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.location.bestlocationstrategy.LocationChangesListener
import extension.TAG


/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O and above
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates in the background. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should NOT be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
class LocationUpdatesBroadcastReceiver constructor() : BroadcastReceiver() {

    //lateinit var locationCallback: LocationChangesListener

    constructor(locationListener: LocationChangesListener?) : this() {
     //   this.locationCallback = locationCallback
    }


    companion object {
        lateinit var locationCallback: LocationChangesListener
        const val ACTION_PROCESS_UPDATES =
            "com.location.bestlocationstrategy." +
                    "PROCESS_UPDATES"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")

        if (intent.action == ACTION_PROCESS_UPDATES) {
            LocationResult.extractResult(intent)?.let { locationResult ->
                locationResult.locations.map { location ->
                    locationCallback?.onBetterLocationAvailable(location)
                }

            }
        }
    }

}