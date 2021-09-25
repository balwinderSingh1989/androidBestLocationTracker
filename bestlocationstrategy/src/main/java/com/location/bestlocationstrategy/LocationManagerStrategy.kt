package com.location.bestlocationstrategy

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

/**
 * Created by Balwinder on 20/Sept/2018.
 */
class LocationManagerStrategy(private val mAppContext: Context) : BaseLocationStrategy, LocationListener {
    private var mLocationManager: LocationManager? = null
    private var mLastLocation: Location? = null
    private var mLocationListener: LocationChangesListener? = null
    private var mUpdatePeriodically = false

    // flag for GPS status
    private var isGPSEnabled = false

    // flag for network status
    private var isNetworkEnabled = false
    override fun startListeningForLocationChanges(locationListener: LocationChangesListener?) {
        mLocationListener = locationListener
        startLocationUpdates()
    }

    override fun stopListeningForLocationChanges() {
        try {
            mLocationManager!!.removeUpdates(this)
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }
    }

    override fun setPeriodicalUpdateEnabled(enable: Boolean) {
        mUpdatePeriodically = enable
    }

    override fun setPeriodicalUpdateTime(time: Long) {
        UPDATE_INTERVAL = time
    }

    override fun setDisplacement(displacement: Long) {
        DISPLACEMENT = displacement
    }

    override val getLatestLocation: Location?
        get() {
            if (mLastLocation == null) {
                mLastLocation = try {
                    mLocationManager!!.getLastKnownLocation(bestProvider)
                } catch (securityException: SecurityException) {
                    return null
                }
            }
            return mLastLocation
        }

    private val bestProvider: String
        get() {
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_HIGH
            return mLocationManager!!.getBestProvider(criteria, false)!!
        }

    override fun initLocationClient() {
        mLocationManager = mAppContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun startLocationUpdates() {
        try {
            // getting GPS status
            isGPSEnabled = mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!
            // getting network status
            isNetworkEnabled = mLocationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)!!


            if (isNetworkEnabled) {
                mLocationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL,
                        DISPLACEMENT.toFloat(), this)
            }

            if (isGPSEnabled) {
                mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL,
                        DISPLACEMENT.toFloat(), this)
            }

        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }
    }

    override fun fetchLocationInBackgroundBelowQ(enable: Boolean) {
     //   TODO("Not yet implemented")
    }

    override fun getBackGroundLocationQandAbove() {
       // TODO("Not yet implemented")
    }

    override fun onLocationChanged(location: Location) {
        mLastLocation = location
        if (mLastLocation != null && mLocationListener != null && LocationUtils.isBetterLocation(location)) {
            mLocationListener!!.onBetterLocationAvailable(location)
        }
        if (!mUpdatePeriodically) {
            stopListeningForLocationChanges()
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        if (mLocationListener != null) mLocationListener!!.onConnectionStatusChanged()
    }

    override fun onProviderEnabled(provider: String) {
        if (mLocationListener != null) mLocationListener!!.onConnected()
    }

    override fun onProviderDisabled(provider: String) {
        if (mLocationListener != null) mLocationListener!!.onFailure(provider)
    }

    companion object {
        private var INSTANCE: LocationManagerStrategy? = null

        // Location updates intervals in sec
        private var UPDATE_INTERVAL: Long = 10000 // 10 sec
        private const val FASTEST_INTERVAL: Long = 5000 // 5 sec
        private var DISPLACEMENT: Long = 10 // 10 meters
        private const val TAG = "LocationManagerStrategy"
        @JvmStatic
        fun getInstance(context: Context): LocationManagerStrategy? {
            if (INSTANCE == null) {
                INSTANCE = LocationManagerStrategy(context)
                INSTANCE!!.initLocationClient()
            }
            return INSTANCE
        }
    }

}