/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.SparseArray;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;

/** ZXing scanner adapter and explicitly unavailable face detection. */
public final class Vision {
    private Vision() {}
    public static final class Frame {
        private Bitmap bitmap;
        private ByteBuffer bytes;
        private int width, height;
        public static final int ROTATION_0 = 0, ROTATION_90 = 1, ROTATION_180 = 2, ROTATION_270 = 3;
        public static final class Builder {
            private final Frame frame=new Frame();
            public Builder setBitmap(Bitmap b) { frame.bitmap=b; return this; }
            public Builder setImageData(ByteBuffer b, int w, int h, int format) {
                if (format != android.graphics.ImageFormat.NV21) throw new IllegalArgumentException("Expected NV21");
                frame.bytes=b; frame.width=w; frame.height=h; return this;
            }
            public Builder setRotation(int rotation) { return this; } // Only used by disabled face detection.
            public Frame build() { return frame; }
        }
    }
    public static final class Barcode {
        public static final int QR_CODE=256, PDF417=2048, DRIVER_LICENSE=12, TEXT=7;
        public int format, valueFormat=TEXT;
        public String rawValue;
        public Point[] cornerPoints=new Point[0];
        public DriverLicense driverLicense; // Structured license parsing is unavailable; raw PDF417 remains readable.
    }
    public static final class DriverLicense {
        public String documentType, issuingCountry, firstName, middleName, lastName, licenseNumber, gender, birthDate, expiryDate;
    }
    public static final class BarcodeDetector {
        private final boolean qrOnly;
        private BarcodeDetector(boolean qrOnly) { this.qrOnly=qrOnly; }
        public boolean isOperational() { return true; }
        public void release() {}
        public static final class Builder {
            private boolean qrOnly;
            public Builder(Context context) {}
            public Builder setBarcodeFormats(int formats) { qrOnly=formats == Barcode.QR_CODE; return this; }
            public BarcodeDetector build() { return new BarcodeDetector(qrOnly); }
        }
        public SparseArray<Barcode> detect(Frame frame) {
            SparseArray<Barcode> found=new SparseArray<>();
            LuminanceSource source;
            if (frame.bitmap != null) {
                int w=frame.bitmap.getWidth(), h=frame.bitmap.getHeight();
                int[] pixels=new int[w*h]; frame.bitmap.getPixels(pixels,0,w,0,0,w,h);
                source=new RGBLuminanceSource(w,h,pixels);
            } else {
                int w=frame.width,h=frame.height;
                if (frame.bytes == null || w <= 0 || h <= 0 || (long)w*h > frame.bytes.remaining()) return found;
                byte[] y=new byte[w*h]; frame.bytes.duplicate().get(y);
                source=new PlanarYUVLuminanceSource(y,w,h,0,0,w,h,false);
            }
            MultiFormatReader reader=new MultiFormatReader();
            EnumMap<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            if (qrOnly) hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            try {
                Result result=reader.decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
                Barcode barcode=new Barcode(); barcode.rawValue=result.getText();
                barcode.format=result.getBarcodeFormat() == BarcodeFormat.QR_CODE ? Barcode.QR_CODE : result.getBarcodeFormat() == BarcodeFormat.PDF_417 ? Barcode.PDF417 : 0;
                // A bounding quadrilateral keeps camera overlays compatible with ZXing finder points.
                ResultPoint[] points=result.getResultPoints();
                float left=source.getWidth(),top=source.getHeight(),right=0,bottom=0;
                boolean hasPoint=false;
                if (points != null) for (ResultPoint point : points) {
                    if (point == null) continue;
                    hasPoint=true; left=Math.min(left,point.getX()); right=Math.max(right,point.getX());
                    top=Math.min(top,point.getY()); bottom=Math.max(bottom,point.getY());
                }
                if (hasPoint) barcode.cornerPoints=new Point[]{new Point((int)left,(int)top),new Point((int)right,(int)top),new Point((int)right,(int)bottom),new Point((int)left,(int)bottom)};
                found.put(0,barcode);
            } catch (ReaderException ignored) {
                // No barcode in this frame. The next camera frame will be tried normally.
            } finally { reader.reset(); }
            return found;
        }
    }
    public static final class FaceDetector {
        public static final int ACCURATE_MODE=1, ALL_LANDMARKS=1, FAST_MODE=0, NO_LANDMARKS=0;
        public static final class Builder {
            public Builder(Context context) {}
            public Builder setMode(int mode) { return this; }
            public Builder setLandmarkType(int type) { return this; }
            public Builder setTrackingEnabled(boolean enabled) { return this; }
            public FaceDetector build() { return new FaceDetector(); }
        }
        public boolean isOperational() { return false; }
        public SparseArray<Face> detect(Frame frame) { return new SparseArray<>(); }
        public void release() {}
    }
    public static final class Face {
        public List<Landmark> getLandmarks() { return Collections.emptyList(); }
        public PointF getPosition() { return new PointF(); }
        public float getWidth() { return 0; }
        public float getHeight() { return 0; }
        public float getEulerY() { return 0; }
        public float getEulerZ() { return 0; }
    }
    public static final class Landmark {
        public static final int LEFT_EYE=4, RIGHT_EYE=10, LEFT_MOUTH=5, RIGHT_MOUTH=11;
        public PointF getPosition() { return new PointF(); }
        public int getType() { return -1; }
    }
}
