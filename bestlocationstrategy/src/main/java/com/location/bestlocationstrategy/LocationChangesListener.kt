package com.location.bestlocationstrategy

import android.location.Location

/**
 * Created by Balwinder on 20/Sept/2018.
 */
interface LocationChangesListener {
    fun onLocationChanged(location: Location?)
    fun onConnected()
    fun onConnectionStatusChanged()
    fun onFailure(failureMessage: String?)
}