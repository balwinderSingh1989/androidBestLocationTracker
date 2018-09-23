# androidBestLocationTracker
Get best available locaiton from android locaiton providers 
android-location-tracker<p align="center">
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
         implementation 'com.github.balwinderSingh1989:androidBestLocationTracker:1.0'
}
```

Don't forget to add the following permissions to your *AndroidManifest.xml*

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

```

 Be aware of `Android Marshmallow`'s new [permission system](https://developer.android.com/preview/features/runtime-permissions.html)

```xml
<permission-group
	android:name="android.permission-group.LOCATION"
	android:label="A label for your permission"
	android:description="A description for the permission" />

```



### Use

As its name says, it's a *simple* library.
To create a tracker you just need to add the below code in your Android Activity/Service

```java
// You can pass an ui Context but it is not mandatory getApplicationContext() would also works
// Be aware if you target android 23, you'll need to handle the runtime-permissions !
// see http://developer.android.com/reference/android/support/v4/content/ContextCompat.html
if (    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
    && ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // You need to ask the user to enable the permissions
} else {
    setupLocation();
}
  private void setupLocation()
    {     baseLocationStrategy = LocationUtils.getLocationStatergy(mContext);
        baseLocationStrategy.setDisplacement(10);
        baseLocationStrategy.setPeriodicalUpdateTime(10000);
        baseLocationStrategy.setPeriodicalUpdateEnabled(true);
        baseLocationStrategy.startListeningForLocationChanges(new LocationChangesListener() {
            @Override
            public void onLocationChanged(Location location) {
                //get best accurate location here on location change
            }
            @Override
            public void onConnected() {
                //best location provider has been connected
            }
            @Override
            public void onConnectionStatusChanged() {
            }
            @Override
            public void onFailure(String s) {
            }
        });
        baseLocationStrategy.startLocationUpdates();
    }

```

And it's done, as soon as a location has been found, it will call the `onLocationChanged()` method and you can do the job.
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

If you have any questions, fell free to send me a mail.
You can also fork this project, or open an issue :)
