package com.location.bestlocationstrategy

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * Created by Balwinder on 1/29/20
 */
class GooglePlayServiceLocationStrategy(private val mAppContext: Context?) : BaseLocationStrategy, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    private var mLastLocation: Location? = null
    private var mLocationListener: LocationChangesListener? = null
    private var mUpdatePeriodically = false
    private var mLocationRequest: LocationRequest? = null
    override fun startListeningForLocationChanges(locationListener: LocationChangesListener?) {
        mLocationListener = locationListener
        if (mGoogleApiClient != null && !mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.connect()
        }
    }

    override fun stopListeningForLocationChanges() {
        mLocationListener = null
        if (mGoogleApiClient!!.isConnected) {
            if (mUpdatePeriodically) stopLocationUpdates()
            mGoogleApiClient!!.disconnect()

            //  mGoogleApiClient.disconnect();
            //INSTANCE = null;
        }
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

    @get:SuppressLint("MissingPermission")
    override val lastLocation: Location?
        get() {
            if (mLastLocation == null) {
                if (mGoogleApiClient != null && mGoogleApiClient!!.isConnected) {
                    mLastLocation = LocationServices.FusedLocationApi
                            .getLastLocation(mGoogleApiClient)
                } else {
                    mLastLocation = LocationManagerStrategy.getInstance(mAppContext!!)?.lastLocation
                }
            }
            LocationUtils.LastKnownLocaiton = mLastLocation
            return mLastLocation
        }

    override fun initLocationClient() {
        mGoogleApiClient = buildGoogleApiClient()
        mLocationRequest = createLocationRequest()
    }

    @Synchronized
    protected fun buildGoogleApiClient(): GoogleApiClient {
        return GoogleApiClient.Builder(mAppContext!!)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build()
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient)
        if (mUpdatePeriodically || mLastLocation == null) {
            startLocationUpdates()
        }
        if (mLocationListener != null) mLocationListener!!.onConnected()
    }

    override fun onConnectionSuspended(i: Int) {
        if (mLocationListener != null) mLocationListener!!.onConnectionStatusChanged()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (mLocationListener != null) mLocationListener!!.onFailure("Failed to connect")
    }

    /**
     * Starting the location updates
     */
    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (mGoogleApiClient!!.isConnected) LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this)
    }

    /**
     * Creating location request object
     */
    protected fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.smallestDisplacement = DISPLACEMENT.toFloat() // 10 meters
        return locationRequest
    }

    /**
     * Stopping location updates
     */
    protected fun stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient!!.isConnected) LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this)
    }

    override fun onLocationChanged(location: Location) {
        if (!mUpdatePeriodically) {
            stopLocationUpdates()
        }
        mLastLocation = location
        if (mLocationListener != null) mLocationListener!!.onLocationChanged(location)
    }

    companion object {
        var mGoogleApiClient: GoogleApiClient? = null
        private var INSTANCE: GooglePlayServiceLocationStrategy? = null

        // Location updates intervals in sec

        private var UPDATE_INTERVAL: Long = 10000 // 10 sec
        private const val FASTEST_INTERVAL: Long = 5000 // 5 sec
        private var DISPLACEMENT: Long = 10 // 10 meters
        private const val TAG = "GooglePlayServiceLocation"

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