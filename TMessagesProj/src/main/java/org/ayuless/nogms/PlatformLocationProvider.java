/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;
import androidx.core.util.Consumer;
import org.telegram.messenger.ILocationServiceProvider;

/** Uses only Android providers; permissions remain controlled by the existing location UI. */
@SuppressLint("MissingPermission")
public final class PlatformLocationProvider implements ILocationServiceProvider {
    private LocationManager manager;
    private final Map<ILocationListener, LocationListener> listeners = new HashMap<>();
    @Override public void init(Context context) { manager=(LocationManager) context.getSystemService(Context.LOCATION_SERVICE); }
    @Override public boolean checkServices() { return manager != null; }
    @Override public ILocationRequest onCreateLocationRequest() { return new Request(); }
    @Override public IMapApiClient onCreateLocationServicesAPI(Context c, IAPIConnectionCallbacks callbacks, IAPIOnConnectionFailedListener failed) {
        return new IMapApiClient() {
            @Override public void connect() { if (manager != null) callbacks.onConnected(null); else failed.onConnectionFailed(); }
            @Override public void disconnect() {}
        };
    }
    @Override public void getLastLocation(Consumer<Location> callback) {
        Location latest=null;
        if (manager != null) {
            for (String provider : manager.getAllProviders()) {
                try {
                    Location candidate=manager.getLastKnownLocation(provider);
                    if (candidate != null && (latest == null || candidate.getElapsedRealtimeNanos() > latest.getElapsedRealtimeNanos())) latest=candidate;
                } catch (SecurityException | IllegalArgumentException ignored) {}
            }
        }
        callback.accept(latest);
    }
    @Override public synchronized void requestLocationUpdates(ILocationRequest request, ILocationListener callback) {
        removeLocationUpdates(callback);
        if (manager == null) return;
        Request r=(Request) request;
        LocationListener listener=new LocationListener() {
            @Override public void onLocationChanged(Location location) { callback.onLocationChanged(location); }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };
        listeners.put(callback, listener);
        for (String provider : manager.getAllProviders()) {
            boolean passive=LocationManager.PASSIVE_PROVIDER.equals(provider);
            boolean gps=LocationManager.GPS_PROVIDER.equals(provider);
            boolean network=LocationManager.NETWORK_PROVIDER.equals(provider);
            if (r.priority == PRIORITY_NO_POWER ? !passive : !(network || gps && r.priority == PRIORITY_HIGH_ACCURACY)) continue;
            try { manager.requestLocationUpdates(provider, Math.max(r.interval, r.fastest), 0, listener, Looper.getMainLooper()); }
            catch (SecurityException | IllegalArgumentException ignored) {}
        }
    }
    @Override public synchronized void removeLocationUpdates(ILocationListener callback) {
        LocationListener listener=listeners.remove(callback);
        if (listener != null && manager != null) {
            try { manager.removeUpdates(listener); } catch (SecurityException ignored) {}
        }
    }
    @Override public void checkLocationSettings(ILocationRequest request, Consumer<Integer> callback) {
        boolean enabled=false;
        if (manager != null) {
            for (String provider : manager.getAllProviders()) {
                if (LocationManager.PASSIVE_PROVIDER.equals(provider)) continue;
                try { enabled |= manager.isProviderEnabled(provider); } catch (IllegalArgumentException ignored) {}
            }
        }
        callback.accept(enabled ? STATUS_SUCCESS : STATUS_RESOLUTION_REQUIRED);
    }
    private static final class Request implements ILocationRequest {
        private int priority=PRIORITY_BALANCED_POWER_ACCURACY;
        private long interval=1000, fastest=0;
        @Override public void setPriority(int value) { priority=value; }
        @Override public void setInterval(long value) { interval=Math.max(0,value); }
        @Override public void setFastestInterval(long value) { fastest=Math.max(0,value); }
    }
}
