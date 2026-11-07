/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import androidx.core.util.Consumer;
import java.util.List;
import org.telegram.messenger.IMapsProvider;
import org.telegram.messenger.R;

/** TODO (#4): replace this unavailable map surface with MapLibre. No tiles are fetched. */
public final class UnavailableMapsProvider implements IMapsProvider {
    @Override public void initializeMaps(Context context) {}
    @Override public IMapView onCreateMapView(Context context) { return new MapView(context); }
    @Override public IMarkerOptions onCreateMarkerOptions() { return new MarkerOptions(); }
    @Override public ICircleOptions onCreateCircleOptions() { return new CircleOptions(); }
    @Override public ILatLngBoundsBuilder onCreateLatLngBoundsBuilder() {
        return new ILatLngBoundsBuilder() {
            private double minLat=90, maxLat=-90, minLon=180, maxLon=-180;
            private boolean empty=true;
            @Override public ILatLngBoundsBuilder include(LatLng p) {
                empty=false; minLat=Math.min(minLat,p.latitude); maxLat=Math.max(maxLat,p.latitude);
                minLon=Math.min(minLon,p.longitude); maxLon=Math.max(maxLon,p.longitude); return this;
            }
            @Override public ILatLngBounds build() {
                final LatLng center=empty ? new LatLng(0,0) : new LatLng((minLat+maxLat)/2,(minLon+maxLon)/2);
                return () -> center;
            }
        };
    }
    @Override public ICameraUpdate newCameraUpdateLatLng(LatLng p) { return new Update(); }
    @Override public ICameraUpdate newCameraUpdateLatLngZoom(LatLng p, float zoom) { return new Update(); }
    @Override public ICameraUpdate newCameraUpdateLatLngBounds(ILatLngBounds b, int padding) { return new Update(); }
    @Override public IMapStyleOptions loadRawResourceStyle(Context c, int res) { return new Style(); }
    @Override public String getMapsAppPackageName() { return ""; }
    @Override public int getInstallMapsString() { return R.string.NoResult; }
    private static final class Update implements ICameraUpdate {}
    private static final class Style implements IMapStyleOptions {}
    private static final class MapView implements IMapView {
        private final TextView view;
        MapView(Context context) {
            view=new TextView(context); view.setGravity(Gravity.CENTER);
            view.setText(R.string.AyulessMapsUnavailable);
        }
        @Override public View getView() { return view; }
        // Do not announce a functional map: existing UI must not use fictitious coordinates.
        @Override public void getMapAsync(Consumer<IMap> callback) {}
        @Override public void onResume() {}
        @Override public void onPause() {}
        @Override public void onCreate(Bundle b) {}
        @Override public void onDestroy() {}
        @Override public void onLowMemory() {}
        @Override public void setOnDispatchTouchEventInterceptor(ITouchInterceptor i) {}
        @Override public void setOnInterceptTouchEventInterceptor(ITouchInterceptor i) {}
        @Override public void setOnLayoutListener(Runnable callback) { view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> callback.run()); }
    }
    private static final class MarkerOptions implements IMarkerOptions {
        @Override public IMarkerOptions position(LatLng p) { return this; }
        @Override public IMarkerOptions icon(Bitmap b) { return this; }
        @Override public IMarkerOptions icon(int r) { return this; }
        @Override public IMarkerOptions anchor(float x, float y) { return this; }
        @Override public IMarkerOptions title(String t) { return this; }
        @Override public IMarkerOptions snippet(String t) { return this; }
        @Override public IMarkerOptions flat(boolean f) { return this; }
    }
    private static final class CircleOptions implements ICircleOptions {
        @Override public ICircleOptions center(LatLng p) { return this; }
        @Override public ICircleOptions radius(double r) { return this; }
        @Override public ICircleOptions strokeColor(int c) { return this; }
        @Override public ICircleOptions fillColor(int c) { return this; }
        @Override public ICircleOptions strokePattern(List<PatternItem> p) { return this; }
        @Override public ICircleOptions strokeWidth(int w) { return this; }
    }
}
