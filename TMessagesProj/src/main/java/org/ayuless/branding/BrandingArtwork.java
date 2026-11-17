// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.branding;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

/** App-owned video watermark, drawn as vectors instead of Telegram's logo atlas. */
public final class BrandingArtwork {
    private BrandingArtwork() {}

    public static Bitmap mark(int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Drawable mark = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.ayuless_mark).mutate();
        mark.setBounds(0, 0, size, size);
        mark.draw(new Canvas(bitmap));
        return bitmap;
    }

    public static Bitmap wordmark() {
        Bitmap bitmap = Bitmap.createBitmap(248, 248, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        Path path = new Path();
        path.addArc(new RectF(-218, -218, 218, 218), 90, -90);
        new Canvas(bitmap).drawTextOnPath("AYULESS", path, pathLengthOffset(), 0, paint);
        return bitmap;
    }

    private static float pathLengthOffset() {
        return (float) (Math.PI * 218 / 4);
    }
}
