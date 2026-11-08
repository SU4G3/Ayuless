/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Switch;

/** Independently drawn settings switches; Telegram owns state, animation and accessibility. */
final class AyulessSettingsSwitch implements Switch.DrawingDelegate {
    private final Theme.ResourcesProvider resourcesProvider;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final double[] lab = new double[3];

    private AyulessSettingsSwitch(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    /** Includes both ordinary toggles and the expandable ghost-mode master switch. */
    static void applyTo(View view, Theme.ResourcesProvider resourcesProvider) {
        if (view instanceof Switch) {
            ((Switch) view).setDrawingDelegate(new AyulessSettingsSwitch(resourcesProvider));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTo(group.getChildAt(i), resourcesProvider);
            }
        }
    }

    private int color(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    private int tone(int color, double lightness) {
        ColorUtils.colorToLAB(color, lab);
        double chromaScale = 32 / Math.max(32, Math.hypot(lab[1], lab[2]));
        return ColorUtils.LABToColor(lightness, lab[1] * chromaScale, lab[2] * chromaScale);
    }

    @Override
    public void draw(Canvas canvas, Switch view) {
        float progress = view.getProgress();
        float cx = view.getWidth() / 2f;
        float cy = view.getHeight() / 2f;
        ColorUtils.colorToLAB(color(Theme.key_windowBackgroundWhite), lab);
        boolean light = lab[0] > 50;
        int base = color(Theme.key_switchTrack);
        int offTrack = tone(base, light ? 90 : 22);
        int offThumb = ColorUtils.blendARGB(offTrack,
                tone(base, light ? 50 : 60), light ? .55f : .8f);
        int onTrack = color(Theme.key_switchTrackChecked);
        int onThumb = color(Theme.key_windowBackgroundWhite);

        float halfWidth = AndroidUtilities.dpf2(21.125f);
        float radius = AndroidUtilities.dpf2(13);
        track.set(cx - halfWidth, cy - radius, cx + halfWidth, cy + radius);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtils.blendARGB(offTrack, onTrack, progress));
        canvas.drawRoundRect(track, radius, radius, paint);

        float stroke = AndroidUtilities.dpf2(1.625f);
        track.inset(stroke / 2, stroke / 2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(Theme.multAlpha(offThumb, 1 - progress));
        canvas.drawRoundRect(track, radius - stroke / 2, radius - stroke / 2, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtils.blendARGB(offThumb, onThumb, progress));
        canvas.drawCircle(cx + AndroidUtilities.dpf2(16.25f) * (progress - .5f), cy,
                AndroidUtilities.dpf2(6.5f + 3.25f * progress), paint);
    }
}
