/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors */
package org.ayuless.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

/** Settings header artwork; uses a brighter rim on dark surfaces and follows live theme changes. */
final class AyulessHeaderLogo extends View implements Theme.Colorable {
    private static final float SIZE = 104;
    private final Theme.ResourcesProvider resources;
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint surface = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tile = new RectF(8.5f, 8.5f, 95.5f, 95.5f);
    private final Path chevron = new Path();

    AyulessHeaderLogo(Context context, Theme.ResourcesProvider resources) {
        super(context);
        this.resources = resources;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1);
        glyph.setStrokeWidth(7);
        glyph.setStrokeCap(Paint.Cap.ROUND);
        glyph.setStrokeJoin(Paint.Join.ROUND);
        chevron.moveTo(28, 60);
        chevron.lineTo(52, 36);
        chevron.lineTo(76, 60);
        updateColors();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateColors();
    }

    @Override public void updateColors() {
        boolean dark = ColorUtils.calculateLuminance(
                Theme.getColor(Theme.key_windowBackgroundGray, resources)) < 0.22;
        surface.setShader(new LinearGradient(20, 12, 84, 96,
                dark ? new int[]{0xFF5C487F, 0xFF3D2E5A, 0xFF2B223E}
                        : new int[]{0xFF4D396A, 0xFF35264D, 0xFF251D41},
                new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
        rim.setShader(new LinearGradient(16, 8, 88, 96,
                dark ? 0xC0C9ADEE : 0x907E639F,
                dark ? 0x607D679E : 0x305A4678, Shader.TileMode.CLAMP));
        halo.setShader(new RadialGradient(52, 52, 52,
                new int[]{dark ? 0x456E52AA : 0x226E52AA, 0x006E52AA},
                new float[]{0.6f, 1}, Shader.TileMode.CLAMP));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth(), getHeight()) / SIZE;
        if (scale <= 0) return;
        int save = canvas.save();
        canvas.translate((getWidth() - SIZE * scale) / 2, (getHeight() - SIZE * scale) / 2);
        canvas.scale(scale, scale);
        canvas.drawCircle(52, 52, 52, halo);
        canvas.drawRoundRect(tile, 26, 26, surface);
        canvas.drawRoundRect(tile, 26, 26, rim);
        glyph.setStyle(Paint.Style.STROKE);
        glyph.setColor(0xFFF8F5FF);
        canvas.drawPath(chevron, glyph);
        glyph.setStyle(Paint.Style.FILL);
        glyph.setColor(0xFFC9B1FF);
        canvas.drawCircle(52, 67, 4.5f, glyph);
        canvas.restoreToCount(save);
    }
}
