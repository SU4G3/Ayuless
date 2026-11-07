/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.graphics.Bitmap;

/** Data shape retained by the disabled sticker subject UI. No model is loaded. */
public final class Learning {
    private Learning() {}
    public static final class Subject {
        private Subject() {}
        public Bitmap getBitmap() { return null; }
        public int getStartX() { return 0; }
        public int getStartY() { return 0; }
        public int getWidth() { return 0; }
        public int getHeight() { return 0; }
    }
}
