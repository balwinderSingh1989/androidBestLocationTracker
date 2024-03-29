package com.location.bestlocationstrategy.foregroundservice

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.location.bestlocationstrategy.R
import extension.TAG
import extension.isApiSandAbove
import java.util.concurrent.TimeUnit


/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 *
 * ref : https://github.com/googlecodelabs/while-in-use-location/blob/master/complete/src/main/java/com/example/android/whileinuselocation/ForegroundOnlyLocationService.kt
 */
class ForegroundOnlyLocationService : Service() {
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    private lateinit var stopLocation: () -> Unit

    private lateinit var requestLocation: () -> Location?


    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification =
            intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("NewApi")
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    @SuppressLint("NewApi")
    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    @SuppressLint("NewApi")
    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange) {
            if (this.isApiSandAbove()) {
                Log.d(TAG, "Start work manager")
                //TODO stop the service here for android API level 31 and above and use work manager instead
                //https://www.ackee.agency/blog/how-to-fetch-location-in-background-on-android
                val periodicWork =
                    PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
                        .addTag(TAG)
                        .build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    Companion.TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    periodicWork
                )
            } else {
                Log.d(TAG, "Start foreground service")
                val notification = generateNotification(currentLocation)
                startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
                serviceRunningInForeground = true
            }
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates(
        requestLocationUpdated: () -> Unit,
        requestLastLocation: () -> Location?,
        stopLocation: () -> Unit
    ) {
        Log.d(TAG, "subscribeToLocationUpdates()")
        this.stopLocation = stopLocation
        this.requestLocation = requestLastLocation
        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        try {
            this.apply {
                //invove location change callback
                requestLocationUpdated.invoke()
            }
        } catch (unlikely: SecurityException) {
            //  SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")
        try {
            stopLocation.invoke()
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun generateNotification(location: Location?): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = "Observing Location..."
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
            )

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        // val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, pendingIntent
        )

//        val activityPendingIntent = PendingIntent.getActivity(
//            this, 0, launchActivityIntent, 0)

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//            .addAction(
//                R.drawable.common_google_signin_btn_icon_dark, "Launch app",
//                activityPendingIntent
//            )
            .addAction(
                R.drawable.common_full_open_on_phone,
                "Cancel",
                servicePendingIntent
            )
            .build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val TAG = "ForegroundOnlyLocationService"

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
    }


    inner class MyWorker constructor(
        private val mContext: Context,
        workerParams: WorkerParameters
    ) : Worker(
        mContext, workerParams
    ) {
        /**
         * Callback for changes in location.
         */
        @SuppressLint("MissingPermission")
        override fun doWork(): Result {
            Log.d(TAG, "doWork: Done")
            Log.d(TAG, "onStartJob: STARTING JOB..")
            requestLocation.invoke()
            return Result.success()
        }

    }

}