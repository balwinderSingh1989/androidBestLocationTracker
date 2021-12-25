/*
 * Copyright © 2021 Balwinder Singh (https://github.com/balwinderSingh1989)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.location.bestlocationstrategy

import android.location.Location
import androidx.lifecycle.Lifecycle

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
    fun startLocationUpdates()

    /**
     * fetchAggresively should be TRUE if you desire to get location via foreground service.
     * if false, android OS will update location few times in hour
     *

     *
     */
    fun shouldFetchWhenInBackground(fetchAggresively: Boolean, lifecycle: Lifecycle?)


    /**
     * To help protect user privacy, Android 11 (API level 30) introduces limitations to when a
     * foreground service can access the device's location, camera, or microphone.
     * When your app starts a foreground service while the app is running in the background, the foreground service has the following limitations:
     *  Unless the user has granted the ACCESS_BACKGROUND_LOCATION permission to your app, the foreground service cannot access location
     * Read : https://developer.android.com/guide/components/foreground-services#while-in-use-restrictions
     *
     */
    fun backgroundLocationPermissionGranted()

}