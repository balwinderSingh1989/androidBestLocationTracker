package com.location.bestlocationstrategy

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.location.bestlocationstrategy.locationBroadCast.LocationUpdatesBroadcastReceiver
import extension.TAG
import extension.hasPermission
import java.util.concurrent.TimeUnit


/**
 * Created by Balwinder on 1/29/20
 */
class GooglePlayServiceLocationStrategy(private val mAppContext: Context?) : BaseLocationStrategy {

    private lateinit var mLocationCallback: LocationCallback
    private var mLastLocation: Location? = null
    private var mLocationListener: LocationChangesListener? = null
    private var mUpdatePeriodically = false
    private var mLocationRequest: LocationRequest? = null
    private var fetchBackgroundLocation: Boolean = false

    // Allows class to cancel the location request if it exits the activity.
    // Typically, you use one cancellation source per lifecycle.
    private var cancellationTokenSource = CancellationTokenSource()


    override fun startListeningForLocationChanges(locationListener: LocationChangesListener?) {
        mLocationListener = locationListener
    }

    /* Creates default PendingIntent for location changes.
    *
    * FOR GETTING LOCATIONS IN BACKGROUND. FOR ANDROID 10 and above use foregrround service instead
    * Note: We use a BroadcastReceiver because on API level 26 and above (Oreo+), Android places
    * limits on Services.
    */
    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val locationUpdatesBroadcastReceiver = LocationUpdatesBroadcastReceiver(mLocationListener)

        val intent = Intent(mAppContext, locationUpdatesBroadcastReceiver.javaClass)

        intent.action = ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(mAppContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun stopListeningForLocationChanges() {
        mLocationListener = null
        if (mUpdatePeriodically) stopLocationUpdates()
    }

    override fun setPeriodicalUpdateEnabled(enable: Boolean) {
        mUpdatePeriodically = enable
    }

    override fun setPeriodicalUpdateTime(time: Long) {
        UPDATE_INTERVAL = if (time < FASTEST_INTERVAL) FASTEST_INTERVAL else time
    }

    override fun setDisplacement(displacement: Long) {
        DISPLACEMENT = displacement
    }


    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     *
     * Note: this method should be called after location permission has been granted.
     */
    @get:SuppressLint("MissingPermission")
    override val lastLocation: Location?
        get() {
            if (mLastLocation == null) {

                mFusedLocationClient?.let {
                    //Try to fetch current location first
                    it.getCurrentLocation(PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                            .addOnCompleteListener { taskLocation ->
                                if (taskLocation.isSuccessful && taskLocation.result != null) {
                                    onLocationChanged(taskLocation.result)

                                } else {


                                    Log.w("TAG", "getLastLocation:exception", taskLocation.exception)
                                    LocationManagerStrategy.getInstance(mAppContext!!)?.lastLocation
                                }
                            }


                } ?: let {
                    LocationManagerStrategy.getInstance(mAppContext!!)?.lastLocation
                }
            }
            LocationUtils.LastKnownLocaiton = mLastLocation
            return mLastLocation
        }

    override fun initLocationClient() {
        mFusedLocationClient = buildFusedLocationProvider()
        mLocationRequest = createLocationRequest()
        onConnected()
    }

    @Synchronized
    private fun buildFusedLocationProvider(): FusedLocationProviderClient? {
        return mAppContext?.let {
            LocationServices.getFusedLocationProviderClient(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onConnected() {
        mFusedLocationClient?.let {
            mLastLocation = lastLocation
            if (mUpdatePeriodically) {
                startLocationUpdates()
            }
            mLocationListener?.onConnected()
        }
    }


    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        mAppContext?.let {

            if (fetchBackgroundLocation && it.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                getBackgroundLocation()
            } else {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {


                } else {
                    //if API is 10 and above use foreground service
                    //https://codelabs.developers.google.com/codelabs/while-in-use-location/index.html?index=..%2F..index#6
                }
            }

        }

    }

    override fun fetchLocationInBackground(enable: Boolean) {
        this.fetchBackgroundLocation = enable
    }

    /**
     * Uses the FusedLocationProvider to start location updates if the correct fine locations are
     * approved.
     *
     * @throws SecurityException if ACCESS_FINE_LOCATION permission is removed before the
     * FusedLocationClient's requestLocationUpdates() has been completed.
     */
    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION"])
    private fun getBackgroundLocation() {
        try {
            // If the PendingIntent is the same as the last request (which it always is), this
            // request will replace any requestLocationUpdates() called before.
            mFusedLocationClient?.requestLocationUpdates(mLocationRequest, locationUpdatePendingIntent)

        } catch (permissionRevoked: SecurityException) {
            // Exception only occurs if the user revokes the FINE location permission before
            // requestLocationUpdates() is finished executing (very rare).
            Log.d(TAG, "Location permission revoked; details: $permissionRevoked")
            throw permissionRevoked
        } catch (exception: Exception) {
            Log.d(TAG, "Location permission error; details: ${exception.message}")
        }

    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest().apply {

            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them slower than requested. You may also receive updates faster than
            // requested if other applications are requesting location at a faster interval.
            //
            // IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
            // receive updates less frequently than this interval when the app is no longer in the
            // foreground.
            interval = UPDATE_INTERVAL

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates faster than this value.
            fastestInterval = FASTEST_INTERVAL

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = DISPLACEMENT.toFloat() // 10 meters
        }
    }

    /**
     * Stopping location updates
     */
    private fun stopLocationUpdates() {
        cancellationTokenSource.cancel()
        mFusedLocationClient?.removeLocationUpdates(locationUpdatePendingIntent)
    }

    //TODO call from onreceive.
    private fun onLocationChanged(location: Location) {
        if (!mUpdatePeriodically) {
            stopLocationUpdates()
        }
        mLastLocation = location
        if (mLocationListener != null) mLocationListener!!.onLocationChanged(location)
    }


    /* Requests location updates from the FusedLocationApi. Note: we don't call this unless location
    * runtime permission has been granted.
    */
    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdatesForBelowQ() {
        Log.i(TAG, "All location settings are satisfied.")
        mFusedLocationClient?.requestLocationUpdates(mLocationRequest,
                mLocationCallback, Looper.myLooper())


    }

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onLocationChanged(locationResult.lastLocation)

            }
        }
    }

    companion object {

        const val ACTION_PROCESS_UPDATES =
                "com.location.bestlocationstrategy." +
                        "PROCESS_UPDATES"

        private var INSTANCE: GooglePlayServiceLocationStrategy? = null

        // Location updates intervals in sec
        private var mFusedLocationClient: FusedLocationProviderClient? = null
        private var UPDATE_INTERVAL: Long = 10000 // 10 sec
        private const val FASTEST_INTERVAL: Long = 5000 // 5 sec
        private var DISPLACEMENT: Long = 10 // 10 meters

        @JvmStatic
        fun getInstance(context: Context?): BaseLocationStrategy? {
            if (INSTANCE == null) {
                INSTANCE = GooglePlayServiceLocationStrategy(context)
                INSTANCE!!.initLocationClient()
            }
            return INSTANCE
        }
    }


}