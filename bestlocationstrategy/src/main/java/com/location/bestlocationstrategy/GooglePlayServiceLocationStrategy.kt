/*
 * Copyright © 2021 Balwinder Singh (https://github.com/balwinderSingh1989)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.location.bestlocationstrategy

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.Lifecycle
import androidx.work.WorkManager
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.location.bestlocationstrategy.foregroundservice.ForeGroundLocationProvider
import com.location.bestlocationstrategy.locationBroadCast.LocationUpdatesBroadcastReceiver
import com.location.bestlocationstrategy.utils.Error
import extension.hasPermission
import java.util.concurrent.TimeUnit


/**
 * Created by Balwinder on 1/29/20
 */
internal class GooglePlayServiceLocationStrategy(
    private val mAppContext: Context,
    private val activity: Activity?,

    ) : BaseLocationStrategy {

    private var mLocationCallback: LocationCallback? = null
    private var mLastLocation: Location? = null
    private var mLocationListener: LocationChangesListener? = null
    private var mUpdatePeriodically = false
    private var mLocationRequest: LocationRequest? = null
    private var lifecycle: Lifecycle? = null


    /**
     * fetch locaton via foreground service
     *
     */
    private var backgroundFetchAggresively: Boolean = false


    // Allows class to cancel the location request if it exits.
    // Typically, you use one cancellation source per lifecycle.
    private var cancellationTokenSource = CancellationTokenSource()


    /** Creates default PendingIntent for location changes.
     *
     * FOR GETTING LOCATIONS IN BACKGROUND. FOR ANDROID 10 and above use foregrround service instead
     * Note: We use a BroadcastReceiver because on API level 26 and above (Oreo+), Android places
     * limits on Services.
     */
    private val locationUpdatePendingIntent: PendingIntent by lazy {
        LocationUpdatesBroadcastReceiver.locationCallback = mLocationListener!!
        val intent = Intent(mAppContext, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(mAppContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
    override val getLatestLocation: Location?
        get() {
            mFusedLocationClient?.let {
                //Try to fetch current location first
                it.getCurrentLocation(PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)

                    .addOnCompleteListener { taskLocation ->

                        if (taskLocation.isSuccessful && taskLocation.result != null) {

                            onLocationChanged(taskLocation.result)

                        } else {
                            Log.e(
                                TAG,
                                "  getLatestLocation ->current location :exception",
                                taskLocation.exception
                            )
                            //current location is not available...try to get last known location
                            it.lastLocation.addOnCompleteListener { taskLastLocation ->
                                if (taskLastLocation.isSuccessful && taskLastLocation.result != null) {
                                    onLocationChanged(taskLastLocation.result)
                                } else {
                                    Log.e(
                                        TAG,
                                        "  getLatestLocation ->last location :exception",
                                        taskLastLocation.exception
                                    )
                                    if (mLastLocation == null) {
                                        LocationManagerStrategy.getInstance(mAppContext)?.getLatestLocation?.let { location ->
                                            onLocationChanged(location)
                                        }
                                    }
                                }
                            }
                        }
                    }


            } ?: let {

                if (mLastLocation == null) {

                    LocationManagerStrategy.getInstance(mAppContext)?.getLatestLocation?.let { location ->
                        onLocationChanged(location)
                    }
                }
            }
            return mLastLocation
        }

    fun initLocationClient() {
        mFusedLocationClient = buildFusedLocationProvider()
        mLocationRequest = createLocationRequest()
        onConnected()
    }

    @Synchronized
    private fun buildFusedLocationProvider(): FusedLocationProviderClient? {
        return mAppContext.let {
            LocationServices.getFusedLocationProviderClient(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onConnected() {
        mFusedLocationClient?.let {
            mLastLocation = getLatestLocation
            if (mUpdatePeriodically) {
                startLocationUpdates()
            }
            mLocationListener?.onConnected()
        }
    }


    /**
     *  Only request Background Location Access if your app has been granted with Foreground Location Access.
     *  If you try to request Foreground and Background Location Access at the same time (i.e., call requestPermissions
     *  API with an array contains ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION),
     *  the Android OS will ignore this request and neither permission will be granted.
     *
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_BACKGROUND_LOCATION"
        ]
    )
    private fun checkIfBackgroundAccessPermissionGranted(): Boolean {

        var accessBackgroundLocationGranted = true

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

            //Android 10 and above..always check if foreground permission first
            if (mAppContext.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                mAppContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                /**
                 * On Android 11 (API level 30) and higher, the system dialog doesn't include the Allow all the time option.
                 * Instead, users must enable background location on a settings page, as shown in figure 3.
                 *
                 */
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

                    if (!mAppContext.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {

                        activity?.let {
                            if (it.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {

                                accessBackgroundLocationGranted = false

                                mLocationListener?.onFailure( Error.ACCESS_BACKGROUND_LOCATION_MISSING_PERMISSION_LAUNCH_SETTINGS)
                            } else {

                                accessBackgroundLocationGranted = false

                                mLocationListener?.onFailure( Error.ACCESS_BACKGROUND_LOCATION_MISSING_PERMISSION)


                            }
                        }
                    } else {

                        accessBackgroundLocationGranted = true

                    }

                } else {
                    //for Q
                    if (mAppContext.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        accessBackgroundLocationGranted = true
                    } else {
                        accessBackgroundLocationGranted = false
                        mLocationListener?.onFailure( Error.ACCESS_BACKGROUND_LOCATION_MISSING_PERMISSION)
                    }
                }


            } else {
                accessBackgroundLocationGranted = false
                mLocationListener?.onFailure( Error.COARSE_OR_FINE_LOCATION_PERMISSION_MISSING)
            }

        }

        return accessBackgroundLocationGranted
    }


    /**
     * Android 10 and 11 give users more control over their apps' access to their device locations.
     * When an app running on Android 11 requests location access, users have four options:
     *  1. Allow all the time
     *  2. Allow only while using the app (in Android 10)
     *  3. One time only (in Android 11)
     *  4. Deny
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"
        ]
    )
    override fun startLocationUpdates() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (mAppContext.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                mAppContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                if (backgroundFetchAggresively) {
                    getLocationViaForeground()
                } else {
                    getBackgroundLocationWithPendingIntent()
                }

            } else {
                mLocationListener?.onFailure( Error.COARSE_OR_FINE_LOCATION_PERMISSION_MISSING)
            }
        } else {
            if (mAppContext.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                mAppContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                if (backgroundFetchAggresively) {
                    getLocationViaForeground()
                } else {
                    if (checkIfBackgroundAccessPermissionGranted()) {
                        getBackgroundLocationWithPendingIntent()
                    }
                }
            }

        }
    }

    private fun getLocationViaForeground() {
        createLocationCallback()
        activity?.let { ac ->
            lifecycle?.let { lifecycle ->
                ForeGroundLocationProvider(
                    ac,
                    ::getLocationWithCallback,
                    ::stopLocationUpdates,
                    ::getLatestLocation
                ).apply {
                    registerLifeCycle(lifecycle)
                }

            }

        }
    }


    override fun shouldFetchWhenInBackground(fetchAggresively: Boolean, lifecycle: Lifecycle?) {
        this.backgroundFetchAggresively = fetchAggresively
        this.lifecycle = lifecycle
    }

    @SuppressLint("MissingPermission") //for Android 10 and above
    override fun backgroundLocationPermissionGranted() {
        if (checkIfBackgroundAccessPermissionGranted()) {
            if (backgroundFetchAggresively) {
                //foreground service already started and should  start yielding location for background as well.
            } else {
                getBackgroundLocationWithPendingIntent()
            }
        }
    }


    /**
     * FOR BELOW Q
     * Uses the FusedLocationProvider to start location updates if the correct fine locations are
     * approved.
     *
     * @throws SecurityException if ACCESS_FINE_LOCATION permission is removed before the
     * FusedLocationClient's requestLocationUpdates() has been completed.
     */
    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"
        ]
    )
    private fun getBackgroundLocationWithPendingIntent() {
        try {
            // If the PendingIntent is the same as the last request (which  always is), this
            // request will replace any requestLocationUpdates() called before.
            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                locationUpdatePendingIntent
            )


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
    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest().apply {
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
        mLocationCallback?.let {
            mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        }
        WorkManager.getInstance().cancelAllWorkByTag(TAG);

    }

    //TODO call from onreceive.
    private fun onLocationChanged(location: Location) {
        if (!mUpdatePeriodically) {
            stopLocationUpdates()
        }
        mLastLocation = location
        mLocationListener?.let {
            if (LocationUtils.isBetterLocation(location))
                it.onBetterLocationAvailable(location)

        }
    }


    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     *
     *
     */
    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    private fun getLocationWithCallback() {
        Log.i(TAG, "All location settings are satisfied.")
        createLocationCallback()
        mFusedLocationClient?.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback, Looper.myLooper()
        )

    }


    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.d(
                    "Location",
                    "mLocationCallback onLocationResult  location = " + locationResult
                )
                onLocationChanged(locationResult.lastLocation)

            }
        }
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

    override fun startListeningForLocationChanges(locationListener: LocationChangesListener?) {
        mLocationListener = locationListener
    }

    var mFusedLocationClient: FusedLocationProviderClient? = null
    companion object {

        const val ACTION_PROCESS_UPDATES =
            "com.location.bestlocationstrategy.PROCESS_UPDATES"

        private var INSTANCE: GooglePlayServiceLocationStrategy? = null

        // Location updates intervals in sec

        private var UPDATE_INTERVAL: Long = 10000 // 10 sec
        private const val FASTEST_INTERVAL: Long = 5000 // 5 sec
        private var DISPLACEMENT: Long = 10 // 10 meters

        const val TAG  = "AndroidBestLocation"

        @JvmStatic
        fun getInstance(context: Context, activity: Activity?): BaseLocationStrategy? {
            if (INSTANCE == null) {
                INSTANCE = GooglePlayServiceLocationStrategy(context, activity)
                INSTANCE!!.initLocationClient()
            }
            return INSTANCE
        }
    }




}