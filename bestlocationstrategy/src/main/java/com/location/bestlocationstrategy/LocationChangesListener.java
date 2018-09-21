package com.location.bestlocationstrategy;

import android.location.Location;

/**
 * Created by Balwinder on 20/Sept/2018.
 */

public interface LocationChangesListener {
    void onLocationChanged(Location location);
    void onConnected();
    void onConnectionStatusChanged();
    void onFailure(String failureMessage);
}
