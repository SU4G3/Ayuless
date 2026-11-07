/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.maps;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.util.Consumer;

import org.maplibre.android.maps.MapView;
import org.telegram.messenger.IMapsProvider;

/** Adapts Telegram's lifecycle (which has no start/stop hooks) to MapLibre. */
final class MapLibreMapView extends FrameLayout implements IMapsProvider.IMapView {
    private final MapView mapView;
    private IMapsProvider.ITouchInterceptor dispatchInterceptor, interceptInterceptor;
    private Runnable layoutListener;
    private MapLibreMap map;
    private boolean created, resumed, destroyed;

    MapLibreMapView(Context context) {
        super(context);
        mapView = new MapView(context);
        addView(mapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mapView.addOnDidFinishRenderingMapListener(fully -> {
            if (map != null && !destroyed) {
                map.onRendered(fully);
            }
        });
    }

    @Override public View getView() { return this; }

    @Override
    public void getMapAsync(Consumer<IMapsProvider.IMap> callback) {
        onCreate(null);
        if (destroyed) return;
        mapView.getMapAsync(nativeMap -> {
            if (destroyed) return;
            if (map == null) {
                map = new MapLibreMap(this, nativeMap);
                map.setResumed(resumed);
            }
            callback.accept(map);
        });
    }

    @Override
    public void onCreate(Bundle state) {
        // Upstream tries to prewarm on a worker thread, then calls again on the UI thread.
        if (Looper.myLooper() != Looper.getMainLooper() || created || destroyed) return;
        mapView.onCreate(state);
        created = true;
    }

    @Override public void onResume() {
        if (destroyed || resumed) return;
        onCreate(null);
        mapView.onStart();
        mapView.onResume();
        resumed = true;
        if (map != null) map.setResumed(true);
    }
    @Override public void onPause() {
        if (!resumed || destroyed) return;
        if (map != null) map.setResumed(false);
        mapView.onPause();
        mapView.onStop();
        resumed = false;
    }
    @Override public void onDestroy() {
        if (destroyed) return;
        onPause();
        destroyed = true;
        if (map != null) map.destroy();
        if (created) mapView.onDestroy();
    }
    @Override public void onLowMemory() {
        if (created && !destroyed) mapView.onLowMemory();
    }
    @Override public void setOnDispatchTouchEventInterceptor(IMapsProvider.ITouchInterceptor value) {
        dispatchInterceptor = value;
    }
    @Override public void setOnInterceptTouchEventInterceptor(IMapsProvider.ITouchInterceptor value) {
        interceptInterceptor = value;
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        return dispatchInterceptor == null ? super.dispatchTouchEvent(event)
                : dispatchInterceptor.onInterceptTouchEvent(event, this::dispatchOriginal);
    }
    private boolean dispatchOriginal(MotionEvent event) { return super.dispatchTouchEvent(event); }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        return interceptInterceptor == null ? super.onInterceptTouchEvent(event)
                : interceptInterceptor.onInterceptTouchEvent(event, this::interceptOriginal);
    }
    private boolean interceptOriginal(MotionEvent event) { return super.onInterceptTouchEvent(event); }
    @Override public void setOnLayoutListener(Runnable callback) { layoutListener = callback; }
    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (layoutListener != null) layoutListener.run();
    }
    @Override public GLSurfaceView getGlSurfaceView() { return findSurface(mapView); }
    private static GLSurfaceView findSurface(View view) {
        if (view instanceof GLSurfaceView) return (GLSurfaceView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                GLSurfaceView result = findSurface(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }
}
