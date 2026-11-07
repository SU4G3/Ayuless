/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.maps;

import android.content.Context;
import android.graphics.Bitmap;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraUpdate;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLngBounds;
import org.telegram.messenger.IMapsProvider;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;

/** OpenStreetMap rendering without Play Services or API credentials. */
public final class MapLibreMapsProvider implements IMapsProvider {
    public static final String LIGHT_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";
    // Use the known public style until a dark endpoint can be verified.
    public static final String DARK_STYLE_URL = LIGHT_STYLE_URL;
    private static boolean initialized;

    @Override
    public void initializeMaps(Context context) {
        synchronized (MapLibreMapsProvider.class) {
            if (!initialized) {
                MapLibre.getInstance(context.getApplicationContext());
                initialized = true;
            }
        }
    }

    @Override
    public IMapView onCreateMapView(Context context) {
        initializeMaps(context);
        return new MapLibreMapView(context);
    }

    @Override public IMarkerOptions onCreateMarkerOptions() { return new MarkerOptions(); }
    @Override public ICircleOptions onCreateCircleOptions() { return new CircleOptions(); }

    @Override
    public ILatLngBoundsBuilder onCreateLatLngBoundsBuilder() {
        return new ILatLngBoundsBuilder() {
            private final List<org.maplibre.android.geometry.LatLng> points = new ArrayList<>();
            @Override public ILatLngBoundsBuilder include(LatLng point) {
                points.add(nativePoint(point));
                return this;
            }
            @Override public ILatLngBounds build() {
                if (points.isEmpty()) {
                    throw new IllegalStateException("Cannot build empty map bounds");
                }
                // MapLibre's builder requires two points, including for a single location.
                LatLngBounds.Builder builder = new LatLngBounds.Builder().includes(points);
                if (points.size() == 1) {
                    builder.include(points.get(0));
                }
                return new Bounds(builder.build());
            }
        };
    }

    @Override public ICameraUpdate newCameraUpdateLatLng(LatLng point) {
        return new Update(CameraUpdateFactory.newLatLng(nativePoint(point)));
    }
    @Override public ICameraUpdate newCameraUpdateLatLngZoom(LatLng point, float zoom) {
        return new Update(CameraUpdateFactory.newLatLngZoom(nativePoint(point), zoom));
    }
    @Override public ICameraUpdate newCameraUpdateLatLngBounds(ILatLngBounds bounds, int padding) {
        return new Update(CameraUpdateFactory.newLatLngBounds(((Bounds) bounds).value, padding));
    }
    @Override public IMapStyleOptions loadRawResourceStyle(Context context, int resId) {
        // Telegram resources contain Google style JSON, not MapLibre style documents.
        return new ThemeStyle();
    }
    @Override public String getMapsAppPackageName() {
        // The embedded renderer requires no separate Maps application.
        return org.telegram.messenger.ApplicationLoader.applicationContext.getPackageName();
    }
    @Override public int getInstallMapsString() { return R.string.NoResult; }

    static org.maplibre.android.geometry.LatLng nativePoint(LatLng point) {
        return new org.maplibre.android.geometry.LatLng(point.latitude, point.longitude);
    }
    static LatLng point(org.maplibre.android.geometry.LatLng point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }
    static final class Update implements ICameraUpdate {
        final CameraUpdate value;
        Update(CameraUpdate value) { this.value = value; }
    }
    private static final class Bounds implements ILatLngBounds {
        final LatLngBounds value;
        Bounds(LatLngBounds value) { this.value = value; }
        @Override public LatLng getCenter() { return point(value.getCenter()); }
    }
    private static final class ThemeStyle implements IMapStyleOptions { }

    static final class MarkerOptions implements IMarkerOptions {
        LatLng position = new LatLng(0, 0);
        Bitmap bitmap;
        int resource;
        float anchorX = 0.5f, anchorY = 1f;
        String title, snippet;
        boolean flat;
        @Override public IMarkerOptions position(LatLng value) { position = value; return this; }
        @Override public IMarkerOptions icon(Bitmap value) { bitmap = value; resource = 0; return this; }
        @Override public IMarkerOptions icon(int value) { resource = value; bitmap = null; return this; }
        @Override public IMarkerOptions anchor(float x, float y) { anchorX = x; anchorY = y; return this; }
        @Override public IMarkerOptions title(String value) { title = value; return this; }
        @Override public IMarkerOptions snippet(String value) { snippet = value; return this; }
        @Override public IMarkerOptions flat(boolean value) { flat = value; return this; }
    }

    static final class CircleOptions implements ICircleOptions {
        LatLng center = new LatLng(0, 0);
        double radius;
        int strokeColor = 0xff000000, fillColor;
        int strokeWidth = 10;
        List<PatternItem> pattern = new ArrayList<>();
        @Override public ICircleOptions center(LatLng value) { center = value; return this; }
        @Override public ICircleOptions radius(double value) { radius = Math.max(0, value); return this; }
        @Override public ICircleOptions strokeColor(int value) { strokeColor = value; return this; }
        @Override public ICircleOptions fillColor(int value) { fillColor = value; return this; }
        @Override public ICircleOptions strokePattern(List<PatternItem> value) {
            pattern = value == null ? new ArrayList<>() : new ArrayList<>(value);
            return this;
        }
        @Override public ICircleOptions strokeWidth(int value) { strokeWidth = Math.max(0, value); return this; }
    }
}
