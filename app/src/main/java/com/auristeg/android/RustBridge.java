package com.auristeg.android;

import android.graphics.Bitmap;

public class RustBridge {
    static {
        System.loadLibrary("auristeg_bridge");
    }

    public static native long loadImage(byte[] bytes);
    public static native int getWidth(long handle);
    public static native int getHeight(long handle);
    public static native byte[] getRgba(long handle);
    public static native byte[] extractBitPlane(long handle, int channel, int bit);
    public static native int fillBitmap(long handle, Bitmap bitmap);
    public static native void freeImage(long handle);
}
