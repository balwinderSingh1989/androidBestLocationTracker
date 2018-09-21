package com.location.bestlocationstrategy;

import android.location.Location;

/**
 * Created by Balwinder on 20/Sept/2018.
 *
 */

public interface BaseLocationStrategy {

    void startListeningForLocationChanges(LocationChangesListener locationListener);

    void stopListeningForLocationChanges();

    void setPeriodicalUpdateEnabled(boolean enable);

    void setPeriodicalUpdateTime(long time);

    void setDisplacement(long displacement);

    Location getLastLocation();

    void initLocationClient();

    void startLocationUpdates();

}
