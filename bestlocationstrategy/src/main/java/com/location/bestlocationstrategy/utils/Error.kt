package com.location.bestlocationstrategy.utils

enum class Error(val description: String) {

    ACCESS_BACKGROUND_LOCATION_MISSING_PERMISSION(
        " Missing permission android.permission.ACCESS_BACKGROUND_LOCATION for Q  +\n" +
                " \"ask for ACCESS_BACKGROUND_LOCATION permission and call backgroundLocationPermissionGranted() \"\n" +
                "                                "
    ),

    ACCESS_BACKGROUND_LOCATION_MISSING_PERMISSION_LAUNCH_SETTINGS(
        "Missing permission android.permission.ACCESS_BACKGROUND_LOCATION for Q  +\n" +
                "    \"launch settings to get background location access and call backgroundLocationPermissionGranted() \""
    ),

    COARSE_OR_FINE_LOCATION_PERMISSION_MISSING(
        "To fetch background location for Q and above..first ask foreground permssion ..." +
                "Missing permission  android.permission.ACCESS_COARSE_LOCATION or " +
                "android.permission.ACCESS_FINE_LOCATION"
    ),
    PROVIDER_DISABLED("plocation provider disabled")

}