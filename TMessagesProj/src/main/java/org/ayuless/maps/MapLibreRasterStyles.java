/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.maps;

import org.json.JSONObject;

/** Keyless public EOX WMTS tiles. See docs/CREDITS.md for permissions and data licences. */
final class MapLibreRasterStyles {
    private MapLibreRasterStyles() {}

    static final String IMAGERY_ATTRIBUTION =
            "Sentinel-2 cloudless – <a href='https://s2maps.eu'>https://s2maps.eu</a> by EOX IT Services GmbH "
            + "(Contains modified Copernicus Sentinel data 2016) · "
            + "<a href='https://creativecommons.org/licenses/by/4.0/'>CC BY 4.0</a> · "
            + "<a href='https://maps.eox.at/'>EOX::Maps</a>";
    static final String LABELS_ATTRIBUTION =
            "Overlay: Data © <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap contributors</a>, "
            + "<a href='https://www.naturalearthdata.com/'>Natural Earth</a>; Rendering © "
            + "<a href='https://maps.eox.at/'>EOX</a> and <a href='https://mapserver.org/'>MapServer</a>";

    static String attribution(boolean hybrid) {
        return IMAGERY_ATTRIBUTION + (hybrid ? "<br>" + LABELS_ATTRIBUTION : "");
    }

    static String json(boolean hybrid) {
        // EOX uses WMTS row/column order (y/x), not the usual XYZ x/y order.
        // Both sources stop at z14 and are overzoomed locally beyond that level.
        return "{\"version\":8,\"sources\":{\"imagery\":"
                + source("s2cloudless_3857", "jpg", IMAGERY_ATTRIBUTION)
                + (hybrid ? ",\"labels\":" + source("overlay_bright_3857", "png", LABELS_ATTRIBUTION) : "")
                + "},\"layers\":[{\"id\":\"imagery\",\"type\":\"raster\",\"source\":\"imagery\"}"
                + (hybrid ? ",{\"id\":\"labels\",\"type\":\"raster\",\"source\":\"labels\"}" : "") + "]}";
    }

    private static String source(String layer, String extension, String attribution) {
        return "{\"type\":\"raster\",\"tileSize\":256,\"minzoom\":0,\"maxzoom\":14,\"tiles\":["
                + JSONObject.quote("https://tiles.maps.eox.at/wmts/1.0.0/" + layer + "/default/g/{z}/{y}/{x}." + extension)
                + "],\"attribution\":" + JSONObject.quote(attribution) + "}";
    }
}
