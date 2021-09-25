package com.location.bestlocationstrategy.foregroundservice

import android.app.Activity
import android.content.*
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.logging.LogManager

class ForeGroundLocationProvider(private val activity: Activity) : LifecycleObserver {

    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null


    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true

        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

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

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
     fun onResume() {
        LocalBroadcastManager.getInstance(activity).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
     fun onPause() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )

            if (location != null) {
                 Log.d("Foreground location:" , "${location.toString()}")
            }
        }
    }

}