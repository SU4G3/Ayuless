/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.maps;

import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ZoomControls;

import androidx.core.util.Consumer;

import org.ayuless.nogms.PlatformLocationProvider;
import org.ayuless.settings.ProfileMapConfig;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.maps.MapLibreMap.CancelableCallback;
import org.maplibre.android.maps.Style;
import org.telegram.messenger.ILocationServiceProvider;
import org.telegram.messenger.IMapsProvider;

import java.util.ArrayList;
import java.util.List;

/** Keeps overlay state independent of the asynchronously loaded style. */
final class MapLibreMap implements IMapsProvider.IMap, IMapsProvider.IUISettings {
    final MapLibreMapView view;
    final org.maplibre.android.maps.MapLibreMap nativeMap;
    final List<MapLibreOverlays.Marker> markers = new ArrayList<>();
    final List<MapLibreOverlays.Circle> circles = new ArrayList<>();
    Style style;
    private String styleUrl;
    private int mapType = IMapsProvider.MAP_TYPE_NORMAL;
    private boolean darkStyle;
    private final TextView rasterAttribution;
    private int styleGeneration, nextId;
    private boolean resumed, destroyed, locationEnabled, locationSubscribed, rendered;
    private Runnable loadedCallback;
    private IMapsProvider.OnMarkerClickListener markerClickListener;
    private Consumer<Location> locationCallback;
    private Location lastLocation;
    private MapLibreOverlays.Marker locationMarker;
    private final PlatformLocationProvider locationProvider = new PlatformLocationProvider();
    private final ILocationServiceProvider.ILocationListener locationListener = this::onLocation;
    private final ZoomControls zoomControls;
    private final ImageButton locationButton;

    MapLibreMap(MapLibreMapView view, org.maplibre.android.maps.MapLibreMap nativeMap) {
        this.view = view;
        this.nativeMap = nativeMap;
        // Telegram chooses its initial zoom as maxZoom - 4, expecting Google's 21.
        nativeMap.setMaxZoomPreference(21);
        locationProvider.init(view.getContext().getApplicationContext());
        // MapLibre reads the OSM/OpenFreeMap attribution from the style's tile sources.
        nativeMap.getUiSettings().setAttributionEnabled(true);
        nativeMap.addOnMapClickListener(point -> {
            PointF screen = nativeMap.getProjection().toScreenLocation(point);
            for (int i = markers.size() - 1; i >= 0; i--) {
                MapLibreOverlays.Marker marker = markers.get(i);
                if (marker == locationMarker || style == null || style.getLayer(marker.id) == null) continue;
                if (!nativeMap.queryRenderedFeatures(screen, marker.id).isEmpty()) {
                    if (markerClickListener == null || !markerClickListener.onClick(marker)) {
                        marker.showInfo();
                    }
                    return true;
                }
            }
            for (MapLibreOverlays.Marker marker : markers) marker.hideInfo();
            return false;
        });
        zoomControls = new ZoomControls(view.getContext());
        zoomControls.setVisibility(View.GONE);
        zoomControls.setOnZoomInClickListener(v -> nativeMap.animateCamera(CameraUpdateFactory.zoomIn()));
        zoomControls.setOnZoomOutClickListener(v -> nativeMap.animateCamera(CameraUpdateFactory.zoomOut()));
        view.addView(zoomControls, new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        locationButton = new ImageButton(view.getContext());
        locationButton.setImageResource(android.R.drawable.ic_menu_mylocation);
        locationButton.setContentDescription("My location");
        locationButton.setVisibility(View.GONE);
        locationButton.setOnClickListener(v -> {
            if (lastLocation != null) {
                nativeMap.animateCamera(CameraUpdateFactory.newLatLng(new org.maplibre.android.geometry.LatLng(
                        lastLocation.getLatitude(), lastLocation.getLongitude())));
            }
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.RIGHT | Gravity.TOP);
        params.setMargins(dp(8), dp(8), dp(8), dp(8));
        view.addView(locationButton, params);
        rasterAttribution = new TextView(view.getContext());
        rasterAttribution.setTextSize(10);
        rasterAttribution.setTextColor(0xff111111);
        rasterAttribution.setLinkTextColor(0xff0645ad);
        rasterAttribution.setBackgroundColor(0xe6ffffff);
        rasterAttribution.setPadding(dp(4), dp(2), dp(4), dp(2));
        rasterAttribution.setMovementMethod(LinkMovementMethod.getInstance());
        rasterAttribution.setVisibility(View.GONE);
        view.addView(rasterAttribution, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.LEFT));
        applyStyle();
    }

    int dp(float value) { return Math.round(value * view.getResources().getDisplayMetrics().density); }
    String nextId() { return "ayuless-overlay-" + nextId++; }

    private void setStyleUrl(String url) {
        if (destroyed || url.equals(styleUrl)) return;
        styleUrl = url;
        style = null;
        rendered = false;
        int generation = ++styleGeneration;
        boolean raster = mapType != IMapsProvider.MAP_TYPE_NORMAL;
        rasterAttribution.setVisibility(raster ? View.VISIBLE : View.GONE);
        if (raster) rasterAttribution.setText(Html.fromHtml(MapLibreRasterStyles.attribution(mapType == IMapsProvider.MAP_TYPE_HYBRID)));
        Style.Builder builder = raster ? new Style.Builder().fromJson(MapLibreRasterStyles.json(mapType == IMapsProvider.MAP_TYPE_HYBRID))
                : new Style.Builder().fromUri(url);
        nativeMap.setStyle(builder, loaded -> {
            if (destroyed || generation != styleGeneration) return;
            style = loaded;
            // Circles belong below markers, including after a theme change.
            for (MapLibreOverlays.Circle circle : circles) circle.attach();
            for (MapLibreOverlays.Marker marker : markers) marker.attach();
            nativeMap.getUiSettings().setAttributionEnabled(true);
        });
    }

    @Override public void setMapType(int type) {
        mapType = ProfileMapConfig.mapTypesEnabled() ? ProfileMapConfig.validType(type) : IMapsProvider.MAP_TYPE_NORMAL;
        applyStyle();
    }
    @Override public void setMapStyle(IMapsProvider.IMapStyleOptions value) {
        darkStyle = value != null;
        applyStyle();
    }
    private void applyStyle() {
        setStyleUrl(mapType == IMapsProvider.MAP_TYPE_NORMAL
                ? (darkStyle ? MapLibreMapsProvider.DARK_STYLE_URL : MapLibreMapsProvider.LIGHT_STYLE_URL)
                : "ayuless-raster-" + mapType);
    }
    private static org.maplibre.android.camera.CameraUpdate update(IMapsProvider.ICameraUpdate update) {
        return ((MapLibreMapsProvider.Update) update).value;
    }
    private static CancelableCallback callback(IMapsProvider.ICancelableCallback callback) {
        if (callback == null) return null;
        return new CancelableCallback() {
            @Override public void onCancel() { callback.onCancel(); }
            @Override public void onFinish() { callback.onFinish(); }
        };
    }
    @Override public void animateCamera(IMapsProvider.ICameraUpdate value) { nativeMap.animateCamera(update(value)); }
    @Override public void animateCamera(IMapsProvider.ICameraUpdate value, IMapsProvider.ICancelableCallback callback) {
        nativeMap.animateCamera(update(value), callback(callback));
    }
    @Override public void animateCamera(IMapsProvider.ICameraUpdate value, int duration, IMapsProvider.ICancelableCallback callback) {
        nativeMap.animateCamera(update(value), Math.max(1, duration), callback(callback));
    }
    @Override public void moveCamera(IMapsProvider.ICameraUpdate value) { nativeMap.moveCamera(update(value)); }
    @Override public float getMaxZoomLevel() { return (float) nativeMap.getMaxZoomLevel(); }
    @Override public float getMinZoomLevel() { return (float) nativeMap.getMinZoomLevel(); }
    @Override public IMapsProvider.CameraPosition getCameraPosition() {
        org.maplibre.android.camera.CameraPosition camera = nativeMap.getCameraPosition();
        return new IMapsProvider.CameraPosition(MapLibreMapsProvider.point(camera.target), (float) camera.zoom);
    }
    @Override public void setOnCameraIdleListener(Runnable callback) {
        if (idleListener != null) nativeMap.removeOnCameraIdleListener(idleListener);
        idleListener = callback == null ? null : callback::run;
        if (idleListener != null) nativeMap.addOnCameraIdleListener(idleListener);
    }
    @Override public void setOnCameraMoveListener(Runnable callback) {
        if (moveListener != null) nativeMap.removeOnCameraMoveListener(moveListener);
        moveListener = callback == null ? null : callback::run;
        if (moveListener != null) nativeMap.addOnCameraMoveListener(moveListener);
    }
    @Override public void setOnCameraMoveStartedListener(IMapsProvider.OnCameraMoveStartedListener callback) {
        if (moveStartedListener != null) nativeMap.removeOnCameraMoveStartedListener(moveStartedListener);
        moveStartedListener = callback == null ? null : reason -> {
            // Both SDKs use 1 = gesture, 2 = API animation, 3 = developer animation.
            callback.onCameraMoveStarted(reason);
        };
        if (moveStartedListener != null) nativeMap.addOnCameraMoveStartedListener(moveStartedListener);
    }
    private org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener idleListener;
    private org.maplibre.android.maps.MapLibreMap.OnCameraMoveListener moveListener;
    private org.maplibre.android.maps.MapLibreMap.OnCameraMoveStartedListener moveStartedListener;

    @Override public void setOnMapLoadedCallback(Runnable callback) {
        loadedCallback = callback;
        if (rendered && callback != null) view.post(this::deliverLoaded);
    }
    void onRendered(boolean fully) {
        if (!fully || style == null || destroyed) return;
        rendered = true;
        deliverLoaded();
    }
    private void deliverLoaded() {
        if (destroyed || !rendered || loadedCallback == null) return;
        Runnable callback = loadedCallback;
        loadedCallback = null;
        callback.run();
    }
    @Override public IMapsProvider.IProjection getProjection() {
        return point -> {
            PointF screen = nativeMap.getProjection().toScreenLocation(MapLibreMapsProvider.nativePoint(point));
            return new Point(Math.round(screen.x), Math.round(screen.y));
        };
    }
    @Override public void setPadding(int left, int top, int right, int bottom) {
        nativeMap.setPadding(left, top, right, bottom);
        // Keep the source attribution within the visible map above Telegram's sheet.
        nativeMap.getUiSettings().setAttributionGravity(Gravity.BOTTOM | Gravity.LEFT);
        nativeMap.getUiSettings().setAttributionMargins(left + dp(8), top + dp(8), right + dp(8), bottom + dp(8));
        FrameLayout.LayoutParams attributionParams = (FrameLayout.LayoutParams) rasterAttribution.getLayoutParams();
        attributionParams.setMargins(left + dp(28), top + dp(4), right + dp(4), bottom + dp(4));
        rasterAttribution.setLayoutParams(attributionParams);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) locationButton.getLayoutParams();
        params.setMargins(left + dp(8), top + dp(8), right + dp(8), bottom + dp(8));
        locationButton.setLayoutParams(params);
        FrameLayout.LayoutParams zoomParams = (FrameLayout.LayoutParams) zoomControls.getLayoutParams();
        zoomParams.setMargins(left, top, right, bottom);
        zoomControls.setLayoutParams(zoomParams);
    }
    @Override public IMapsProvider.IMarker addMarker(IMapsProvider.IMarkerOptions options) {
        MapLibreOverlays.Marker marker = new MapLibreOverlays.Marker(this, (MapLibreMapsProvider.MarkerOptions) options);
        markers.add(marker);
        marker.attach();
        return marker;
    }
    @Override public IMapsProvider.ICircle addCircle(IMapsProvider.ICircleOptions options) {
        MapLibreOverlays.Circle circle = new MapLibreOverlays.Circle(this, (MapLibreMapsProvider.CircleOptions) options);
        circles.add(circle);
        circle.attach();
        return circle;
    }
    @Override public void setOnMarkerClickListener(IMapsProvider.OnMarkerClickListener listener) { markerClickListener = listener; }
    @Override public IMapsProvider.IUISettings getUiSettings() { return this; }
    @Override public void setZoomControlsEnabled(boolean enabled) { zoomControls.setVisibility(enabled ? View.VISIBLE : View.GONE); }
    @Override public void setMyLocationButtonEnabled(boolean enabled) { locationButton.setVisibility(enabled ? View.VISIBLE : View.GONE); }
    @Override public void setCompassEnabled(boolean enabled) { nativeMap.getUiSettings().setCompassEnabled(enabled); }

    @Override public void setMyLocationEnabled(boolean enabled) {
        // Retry after permission grants even when a prior request was denied.
        if (enabled && locationSubscribed) {
            locationProvider.removeLocationUpdates(locationListener);
            locationSubscribed = false;
        }
        locationEnabled = enabled;
        updateLocationSubscription();
        if (!enabled && locationMarker != null) {
            locationMarker.remove();
            locationMarker = null;
        }
    }
    @Override public void setOnMyLocationChangeListener(Consumer<Location> callback) {
        locationCallback = callback;
        if (locationEnabled && resumed && lastLocation != null && callback != null) {
            callback.accept(new Location(lastLocation));
        }
    }
    void setResumed(boolean value) {
        resumed = value;
        updateLocationSubscription();
    }
    private void updateLocationSubscription() {
        boolean subscribe = resumed && locationEnabled && !destroyed;
        if (subscribe == locationSubscribed) return;
        locationSubscribed = subscribe;
        if (subscribe) {
            ILocationServiceProvider.ILocationRequest request = locationProvider.onCreateLocationRequest();
            request.setPriority(ILocationServiceProvider.PRIORITY_HIGH_ACCURACY);
            request.setInterval(1000);
            request.setFastestInterval(1000);
            locationProvider.requestLocationUpdates(request, locationListener);
            locationProvider.getLastLocation(location -> { if (location != null) onLocation(location); });
        } else {
            locationProvider.removeLocationUpdates(locationListener);
        }
    }
    private void onLocation(Location location) {
        if (destroyed || !resumed || !locationEnabled) return;
        lastLocation = new Location(location);
        IMapsProvider.LatLng point = new IMapsProvider.LatLng(location.getLatitude(), location.getLongitude());
        if (locationMarker == null) {
            MapLibreMapsProvider.MarkerOptions options = new MapLibreMapsProvider.MarkerOptions();
            options.position(point).anchor(0.5f, 0.5f).icon(MapLibreOverlays.locationIcon(dp(20)));
            locationMarker = (MapLibreOverlays.Marker) addMarker(options);
        } else {
            locationMarker.setPosition(point);
        }
        if (locationCallback != null) locationCallback.accept(new Location(location));
    }
    void destroy() {
        destroyed = true;
        updateLocationSubscription();
        loadedCallback = null;
        locationCallback = null;
        markerClickListener = null;
        markers.clear();
        circles.clear();
        locationMarker = null;
        style = null;
    }
}
