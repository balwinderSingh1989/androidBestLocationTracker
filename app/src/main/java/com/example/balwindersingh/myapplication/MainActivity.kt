package com.example.balwindersingh.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.location.bestlocationstrategy.BaseLocationStrategy
import com.location.bestlocationstrategy.LocationChangesListener
import com.location.bestlocationstrategy.LocationUtils.getLocationStatergy
import com.location.bestlocationstrategy.utils.Error
import extension.TAG
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    var baseLocationStrategy: BaseLocationStrategy? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        btnBackgroundLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            ) {
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