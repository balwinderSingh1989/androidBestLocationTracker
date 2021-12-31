package com.location.bestlocationstrategy

import android.location.Location
import com.location.bestlocationstrategy.utils.Error

/**
 * Created by Balwinder on 20/Sept/2018.
 */
interface LocationChangesListener {
    fun onBetterLocationAvailable(location: Location?)
    fun onConnected()
    fun onConnectionStatusChanged()
    fun onFailure(error: Error)
}