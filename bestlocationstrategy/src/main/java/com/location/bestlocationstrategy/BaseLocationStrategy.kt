package com.location.bestlocationstrategy

import android.location.Location

/**
 * Created by Balwinder on 20/Sept/2018.
 *
 */
interface BaseLocationStrategy {
    fun startListeningForLocationChanges(locationListener: LocationChangesListener?)
    fun stopListeningForLocationChanges()
    fun setPeriodicalUpdateEnabled(enable: Boolean)


    /**
    Sets the desired interval for active location updates. This interval is inexact. You
    * may not receive updates at all if no location sources are available, or you may
    * receive them slower than requested. You may also receive updates faster than
    * requested if other applications are requesting location at a faster interval.
    *
     * IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
    * receive updates less frequently than this interval when the app is no longer in the
    foreground.
    */

    fun setPeriodicalUpdateTime(time: Long)
    fun setDisplacement(displacement: Long)
    val getLatestLocation: Location?
    fun initLocationClient()
    fun startLocationUpdates()
    fun fetchLocationInBackground(enable: Boolean)
}