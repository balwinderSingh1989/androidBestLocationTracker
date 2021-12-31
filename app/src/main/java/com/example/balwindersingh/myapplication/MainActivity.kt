package com.example.balwindersingh.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.balwindersingh.myapplication.databinding.ActivityMainBinding
import com.location.bestlocationstrategy.BaseLocationStrategy
import com.location.bestlocationstrategy.LocationChangesListener
import com.location.bestlocationstrategy.LocationUtils.getLocationStatergy
import com.location.bestlocationstrategy.utils.Error
import extension.TAG


class MainActivity : AppCompatActivity() {


    var baseLocationStrategy: BaseLocationStrategy? = null
    lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        baseLocationStrategy = getLocationStatergy(this, this)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
        // You need to ask the user to enable the permissions
        {
            startLocationUpdate()
        } else {
            this.requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 100
            )
        }


        /**
         * this is required for android 10 and above and app should ask for ACCESS_BACKGROUND_LOCATION
         * to fetch location is background.
         *
         */
        binding.btnBackgroundLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            ) {

                /**
                 * inform the library that permission has been granted and can start fetching locaiton in background as well.
                 *
                 */
                baseLocationStrategy?.backgroundLocationPermissionGranted()
            } else {
                this.requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    ), 200
                )
            }
        }
    }

    fun startLocationUpdate() {
        baseLocationStrategy?.apply {
            setDisplacement(5)
            setPeriodicalUpdateTime(1000)
            setPeriodicalUpdateEnabled(true)

            /**
             * fetchAggresively = true would drain more battery as this will launch foregeound service and
             * would fetch location aggressively (android default is few times in a hour).
             *
             * For android API 31 and above workmanger would be used to fetch background location.
             *
             *
             *
             * lifecycle : lib use this to check if acitivity is in background or not to start foreground or workmanger
             * make user this is always called from root activity of your applicaiton.
             *
             */

            shouldFetchWhenInBackground(fetchAggresively = true, lifecycle)

            startListeningForLocationChanges(object : LocationChangesListener {
                override fun onBetterLocationAvailable(location: Location?) {
                    Log.d(TAG, "onBetterLocationAvailable  ${location.toString()}")
                }

                override fun onConnected() {
                    Log.d(TAG, "onConnected")
                }

                override fun onConnectionStatusChanged() {}
                override fun onFailure(s: Error) {


                }
            })
        }
        baseLocationStrategy?.startLocationUpdates()
    }
}