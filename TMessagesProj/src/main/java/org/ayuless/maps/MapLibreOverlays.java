/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;
import org.telegram.messenger.IMapsProvider;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.maplibre.android.style.layers.PropertyFactory.*;

/** Style layers provide marker rotation/anchoring and circles measured in metres. */
final class MapLibreOverlays {
    private MapLibreOverlays() { }

    static Bitmap locationIcon(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size * 0.48f, paint);
        paint.setColor(0xff4285f4);
        canvas.drawCircle(size / 2f, size / 2f, size * 0.35f, paint);
        return bitmap;
    }

    static final class Marker implements IMapsProvider.IMarker {
        final MapLibreMap map;
        final String id;
        private IMapsProvider.LatLng position;
        private Object tag;
        private Bitmap bitmap;
        private final float anchorX, anchorY;
        private final boolean flat;
        private final String title, snippet;
        private float rotation;
        private boolean removed, infoVisible;
        private GeoJsonSource source;
        private SymbolLayer layer;

        Marker(MapLibreMap map, MapLibreMapsProvider.MarkerOptions options) {
            this.map = map;
            id = map.nextId();
            position = options.position;
            anchorX = options.anchorX;
            anchorY = options.anchorY;
            flat = options.flat;
            title = options.title;
            snippet = options.snippet;
            bitmap = options.bitmap == null ? resourceBitmap(options.resource == 0 ? R.drawable.map_pin2 : options.resource)
                    : copyBitmap(options.bitmap);
        }

        private Bitmap copyBitmap(Bitmap original) {
            Bitmap copy = original.copy(Bitmap.Config.ARGB_8888, false);
            // MapLibre uses bitmap density as the style image's pixel ratio.
            copy.setDensity(map.view.getResources().getDisplayMetrics().densityDpi);
            return copy;
        }
        private Bitmap resourceBitmap(int resource) {
            Drawable drawable = ResourcesCompat.getDrawable(map.view.getResources(), resource, map.view.getContext().getTheme());
            if (drawable == null) throw new IllegalArgumentException("Missing marker drawable");
            Bitmap result = Bitmap.createBitmap(Math.max(1, drawable.getIntrinsicWidth()),
                    Math.max(1, drawable.getIntrinsicHeight()), Bitmap.Config.ARGB_8888);
            result.setDensity(map.view.getResources().getDisplayMetrics().densityDpi);
            drawable.setBounds(0, 0, result.getWidth(), result.getHeight());
            drawable.draw(new Canvas(result));
            return result;
        }
        void attach() {
            if (removed || map.style == null) return;
            source = new GeoJsonSource(id, Point.fromLngLat(position.longitude, position.latitude));
            map.style.addSource(source);
            map.style.addImage(id, bitmap);
            layer = new SymbolLayer(id, id).withProperties(
                    iconImage(id), iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconAllowOverlap(true), iconIgnorePlacement(true),
                    iconRotationAlignment(flat ? Property.ICON_ROTATION_ALIGNMENT_MAP : Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                    iconPitchAlignment(flat ? Property.ICON_PITCH_ALIGNMENT_MAP : Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                    iconRotate(rotation), textField(infoText()), textSize(12f),
                    textOffset(new Float[] {0f, -3f}), textAllowOverlap(true), textIgnorePlacement(true),
                    textColor(Color.BLACK), textHaloColor(Color.WHITE), textHaloWidth(2f),
                    textOpacity(infoVisible ? 1f : 0f));
            updateOffset();
            map.style.addLayer(layer);
        }
        private void updateOffset() {
            if (layer == null || map.style == null || removed) return;
            float ratio = map.view.getResources().getDisplayMetrics().density;
            layer.setProperties(iconOffset(new Float[] {
                    (0.5f - anchorX) * bitmap.getWidth() / ratio,
                    (0.5f - anchorY) * bitmap.getHeight() / ratio}));
        }
        private String infoText() {
            String text = title == null ? "" : title;
            if (snippet != null && !snippet.isEmpty()) text += (text.isEmpty() ? "" : "\n") + snippet;
            return text;
        }
        void showInfo() {
            for (Marker marker : map.markers) marker.hideInfo();
            infoVisible = true;
            if (layer != null && map.style != null) layer.setProperties(textOpacity(1f));
        }
        void hideInfo() {
            infoVisible = false;
            if (layer != null && map.style != null) layer.setProperties(textOpacity(0f));
        }
        @Override public Object getTag() { return tag; }
        @Override public void setTag(Object value) { tag = value; }
        @Override public IMapsProvider.LatLng getPosition() { return position; }
        @Override public void setPosition(IMapsProvider.LatLng value) {
            position = value;
            if (!removed && map.style != null && source != null) {
                source.setGeoJson(Point.fromLngLat(value.longitude, value.latitude));
            }
        }
        @Override public void setRotation(int value) {
            rotation = value;
            if (!removed && map.style != null && layer != null) layer.setProperties(iconRotate(rotation));
        }
        private void replaceBitmap(Bitmap value) {
            if (removed) return;
            bitmap = value;
            if (map.style != null && layer != null) {
                map.style.addImage(id, bitmap);
                updateOffset();
            }
        }
        @Override public void setIcon(Bitmap value) { replaceBitmap(copyBitmap(value)); }
        @Override public void setIcon(int value) { replaceBitmap(resourceBitmap(value)); }
        @Override public void remove() {
            if (removed) return;
            removed = true;
            map.markers.remove(this);
            if (map.style != null && map.style.getLayer(id) != null) {
                map.style.removeLayer(id);
                map.style.removeSource(id);
                map.style.removeImage(id);
            }
            source = null;
            layer = null;
        }
    }

    static final class Circle implements IMapsProvider.ICircle {
        private final MapLibreMap map;
        private final String id;
        private IMapsProvider.LatLng center;
        private double radius;
        private int stroke, fill;
        private final float width;
        private final List<IMapsProvider.PatternItem> pattern;
        private boolean removed;
        private GeoJsonSource source;
        private FillLayer fillLayer;
        private LineLayer lineLayer;

        Circle(MapLibreMap map, MapLibreMapsProvider.CircleOptions options) {
            this.map = map;
            id = map.nextId();
            center = options.center;
            radius = options.radius;
            stroke = options.strokeColor;
            fill = options.fillColor;
            width = options.strokeWidth;
            pattern = new ArrayList<>(options.pattern);
        }
        void attach() {
            if (removed || map.style == null) return;
            source = new GeoJsonSource(id, geometry());
            map.style.addSource(source);
            fillLayer = new FillLayer(id + "-fill", id);
            lineLayer = new LineLayer(id + "-stroke", id);
            setColors();
            lineLayer.setProperties(lineWidth(width / map.view.getResources().getDisplayMetrics().density));
            Float[] dash = dashArray();
            if (dash.length > 0) lineLayer.setProperties(lineDasharray(dash));
            // New circles must also stay below existing live-location markers.
            String before = null;
            for (Marker marker : map.markers) {
                if (map.style.getLayer(marker.id) != null) { before = marker.id; break; }
            }
            if (before == null) {
                map.style.addLayer(fillLayer);
                map.style.addLayer(lineLayer);
            } else {
                map.style.addLayerBelow(fillLayer, before);
                map.style.addLayerBelow(lineLayer, before);
            }
        }
        private void setColors() {
            if (fillLayer != null) fillLayer.setProperties(fillColor(fill | 0xff000000), fillOpacity(Color.alpha(fill) / 255f));
            if (lineLayer != null) lineLayer.setProperties(lineColor(stroke | 0xff000000), lineOpacity(Color.alpha(stroke) / 255f));
        }
        private Float[] dashArray() {
            List<Float> lengths = new ArrayList<>();
            boolean expectDash = true;
            float total = 0;
            for (IMapsProvider.PatternItem item : pattern) {
                boolean dash = item instanceof IMapsProvider.PatternItem.Dash;
                int length;
                if (dash) length = ((IMapsProvider.PatternItem.Dash) item).length;
                else if (item instanceof IMapsProvider.PatternItem.Gap) length = ((IMapsProvider.PatternItem.Gap) item).length;
                else continue;
                if (dash != expectDash) { lengths.add(0f); expectDash = !expectDash; }
                float normalized = Math.max(0, length) / Math.max(1f, width);
                lengths.add(normalized);
                total += normalized;
                expectDash = !expectDash;
            }
            if (total == 0) return new Float[0];
            if (lengths.size() % 2 != 0) lengths.add(0f);
            return lengths.toArray(new Float[0]);
        }
        private Polygon geometry() {
            // A geodesic polygon keeps the radius in metres at every latitude/zoom.
            double angular = radius / 6371008.8;
            double latitude = Math.toRadians(center.latitude);
            double longitude = Math.toRadians(center.longitude);
            List<Point> ring = new ArrayList<>();
            for (int i = 0; i < 128; i++) {
                double bearing = 2 * Math.PI * i / 128;
                double sinLat = Math.sin(latitude) * Math.cos(angular)
                        + Math.cos(latitude) * Math.sin(angular) * Math.cos(bearing);
                double lat = Math.asin(Math.max(-1, Math.min(1, sinLat)));
                double lon = longitude + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(latitude),
                        Math.cos(angular) - Math.sin(latitude) * Math.sin(lat));
                ring.add(Point.fromLngLat(Math.toDegrees(lon), Math.toDegrees(lat)));
            }
            ring.add(ring.get(0));
            return Polygon.fromLngLats(Collections.singletonList(ring));
        }
        private void updateGeometry() {
            if (!removed && map.style != null && source != null) source.setGeoJson(geometry());
        }
        @Override public void setStrokeColor(int value) { stroke = value; if (!removed && map.style != null) setColors(); }
        @Override public void setFillColor(int value) { fill = value; if (!removed && map.style != null) setColors(); }
        @Override public double getRadius() { return radius; }
        @Override public void setRadius(double value) { radius = Math.max(0, value); updateGeometry(); }
        @Override public void setCenter(IMapsProvider.LatLng value) { center = value; updateGeometry(); }
        @Override public void remove() {
            if (removed) return;
            removed = true;
            map.circles.remove(this);
            if (map.style != null && map.style.getLayer(id + "-fill") != null) {
                map.style.removeLayer(id + "-stroke");
                map.style.removeLayer(id + "-fill");
                map.style.removeSource(id);
            }
            source = null;
            fillLayer = null;
            lineLayer = null;
        }
    }
}
