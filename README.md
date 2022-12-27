# androidBestLocationTracker
Get best available locaiton from android locaiton providers 

use latest version 2.0.9


Whats comming next ? 

To make location data as stream and not callback. Will be incorporating flow{} to this lib.

<kbd>
<img src="https://3c1703fe8d.site.internapcdn.net/newman/gfx/news/hires/2018/location.jpg" alt="Live Location Sharing" width="300">
</kbd>
</p>
========================

Android Best Location Tracker is an Android library that helps you get user best  location with a object named `BaseLocationStrategy`
that would give a accurate location using accuracy alogirthm.  



### Installation

Add this to your `build.gradle` file

```gradle
repositories {
    maven {
        url "https://jitpack.io"
    }
}

dependencies {
         implementation 'com.github.balwinderSingh1989:androidBestLocationTracker:2.0.6'
}
```

Don't forget to add the following permissions to your *AndroidManifest.xml*

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
add below if you need locaton in background for android Q and above. Check out this blog
https://www.ackee.agency/blog/how-to-fetch-location-in-background-on-android
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```


### Use


To create a tracker you just need to add the below code in your Android Activity/Service

```java
// You can pass an ui Context but it is not mandatory getApplicationContext() would also works
// Be aware if you target android 23, you'll need to handle the runtime-permissions !
// see http://developer.android.com/reference/android/support/v4/content/ContextCompat.html
if (    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
    || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // You need to ask the user to enable the permissions
} else {
    setupLocation();
}


  private void setupLocation()
    {
         baseLocationStrategy = getLocationStatergy(this, this)

         baseLocationStrategy?.apply {
                  setDisplacement(5)
                  setPeriodicalUpdateTime(1000)
                  setPeriodicalUpdateEnabled(true)

                  shouldFetchWhenInBackground(fetchAggresively = true, lifecycle)

                  /**
                  NOTE :SET  fetchAggresively true if you need location in background faster than android default (which is few times in a hour).
                  when this is TRUE : for android API  < 31, location will be fetched by foreground service and for above lib will use workmanger


                  REMEMBER : You would need permission <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" /> to fecth location in
                  background for Adnroid 10 and above



                  lifecycle  = set this to activity as lib would use this lifecycle to bind the foreground service and would start the service once the activity is not visible
                  (i.e activity is in background)

                  **/

                  startListeningForLocationChanges(object : LocationChangesListener {
                      override fun onBetterLocationAvailable(location: Location?) {
                          Log.d(TAG, "onBetterLocationAvailable  ${location.toString()}")
                      }

                      override fun onConnected() {
                          Log.d(TAG, "onConnected")
                      }

                      override fun onConnectionStatusChanged() {}
                      override fun onFailure(s: Error) {
                         //check error codes here

                      }
                  })
                  baseLocationStrategy?.startLocationUpdates()
  }



//also from android 10 and above , one cannot ask foreground and background location at the same time. Hence for background location one should have a proper use case
//and UI option to trigger background location. Once you have decided the use case, you can call below fun to let lib known that ACCESS_BACKGROUND_LOCATION is granted, so that
//lib can start providing you location when your app is in background.

     btnStartBackgroundFetch.setOnClickListener {
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
                    ), REQUEST_CODE
                )
            }
        }




```

And it's done, as soon as a location gets available, it will call the `onLocationChanged()` with latest and best available locaition.
Also, to get lastKnown location any time (this should only we called if )

```java
baseLocationStrategy.getLastLocation();
```

### Manage the tracker

By default, after a `baseLocationStrategy` is created, it automatically starts listening to updates... and never stops.
`LocationTracker` has two methods to *start* and *stop* listening for updates.

If you want to *stop* listening for updates, just call the `  baseLocationStrategy.stopListeningForLocationChanges()` method.
For example, if you need a *one shot* position, you can do that:

```java
 baseLocationStrategy.startListeningForLocationChanges(new LocationChangesListener() {
            @Override
            public void onLocationChanged(Location location) {
                //get best accurate location here and here you can stop
                baseLocationStrategy.stopListeningForLocationChanges()
            }
            @Override
            public void onConnected() {
                //best location provider has been connected and you will surely get location changes now
            }
            @Override
            public void onConnectionStatusChanged() {
            }
            @Override
            public void onFailure(String s) {
            }
        });
        baseLocationStrategy.startLocationUpdates();
```

You can also do it in the `onPause()` Activity method if you want.

```java
@Override
protected void onPause() {
	if(baseLocationStrategy != null) {
		baseLocationStrategy.stopListeningForLocationChanges();
	}
	super.onPause();
}
```

REMEMBER! A `LocationTracker` never stops untill you tell it to do so.

You may want to start listening for updates after all. To do that, `LocationTracker` has a public method named `startLocationUpdates()`, call it when you want.

For example, in the `onResume()` Activity method:
```java
@Override
protected void onResume() {
	if(baseLocationStrategy != null) {
		baseLocationStrategy.startLocationUpdates();
	}
	super.onResume();
}
```

### Contact & Questions

If you have any questions, fell free to send me an email at er.vicky1989@gmail.com
You can also fork this project, or open an issue :)
