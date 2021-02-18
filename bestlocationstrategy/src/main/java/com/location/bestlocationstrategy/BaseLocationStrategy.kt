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
    fun setPeriodicalUpdateTime(time: Long)
    fun setDisplacement(displacement: Long)
    val lastLocation: Location?
    fun initLocationClient()
    fun startLocationUpdates()
}