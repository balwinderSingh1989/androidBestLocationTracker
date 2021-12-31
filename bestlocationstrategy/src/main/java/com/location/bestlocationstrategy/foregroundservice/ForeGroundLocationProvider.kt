package com.location.bestlocationstrategy.foregroundservice

import android.app.Activity
import android.content.*
import android.location.Location
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class ForeGroundLocationProvider(
    private val activity: Activity,
    private var requestLocationUpdate: () -> Unit,
    private var stopLocation: () -> Unit,
    private var getLastLocation: () -> Location?
) : LifecycleObserver {

    fun registerLifeCycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null


    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
            subscribeLocation()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    fun subscribeLocation() {
        foregroundOnlyLocationService?.let {
            it.subscribeToLocationUpdates(requestLocationUpdate, getLastLocation, stopLocation)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        val serviceIntent = Intent(activity, ForegroundOnlyLocationService::class.java)
        activity.bindService(
            serviceIntent,
            foregroundOnlyServiceConnection,
            Context.BIND_AUTO_CREATE
        )

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            activity.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
    }


}