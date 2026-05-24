package com.auristeg.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_DIM = 4096;

    private ImageView imageView;
    private TextView statusText;
    private Button btnOpen, btnOriginal;
    private RadioGroup rgChannel, rgBit;
    private long imageHandle = -1;

    private final Matrix matrix = new Matrix();
    private float scale = 1f;
    private float minScale = 1f;
    private int imgW, imgH;

    private ScaleGestureDetector scaleDetector;
    private float lastTouchX, lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 35) {
            findViewById(android.R.id.content).setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets si = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(si.left, si.top, si.right, si.bottom);
                return insets;
            });
        }

        imageView = findViewById(R.id.image_view);
        statusText = findViewById(R.id.status);
        btnOpen = findViewById(R.id.btn_open);
        btnOriginal = findViewById(R.id.btn_original);
        rgChannel = findViewById(R.id.rg_channel);
        rgBit = findViewById(R.id.rg_bit);

        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        btnOpen.setOnClickListener(v -> openFilePicker());
        btnOriginal.setOnClickListener(v -> showOriginal());

        rgChannel.setOnCheckedChangeListener((group, checkedId) -> updateBitPlane());
        rgBit.setOnCheckedChangeListener((group, checkedId) -> updateBitPlane());

        imageView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    activePointerId = event.getPointerId(0);
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (activePointerId == MotionEvent.INVALID_POINTER_ID) break;
                    int idx = event.findPointerIndex(activePointerId);
                    if (idx < 0) break;
                    float x = event.getX(idx);
                    float y = event.getY(idx);
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    matrix.postTranslate(dx, dy);
                    lastTouchX = x;
                    lastTouchY = y;
                    imageView.setImageMatrix(matrix);
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
            }
            return true;
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) loadImage(uri);
        }
    }

    private void loadImage(Uri uri) {
        try {
            if (imageHandle >= 0) {
                RustBridge.freeImage(imageHandle);
                imageHandle = -1;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream preview = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(preview, null, opts);
            if (preview != null) preview.close();

            int sample = 1;
            while (opts.outWidth / sample > MAX_DIM || opts.outHeight / sample > MAX_DIM) {
                sample *= 2;
            }

            InputStream is = getContentResolver().openInputStream(uri);
            byte[] fileBytes = readAllBytes(is);
            if (is != null) is.close();

            long handle = RustBridge.loadImage(fileBytes);
            if (handle < 0) {
                statusText.setText("Failed to decode image");
                return;
            }
            imageHandle = handle;
            imgW = RustBridge.getWidth(handle);
            imgH = RustBridge.getHeight(handle);

            showOriginal();
            fitToView();
            statusText.setText(uri.getLastPathSegment() + "  " + imgW + "x" + imgH);
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }

    private void showOriginal() {
        if (imageHandle < 0) return;
        Bitmap bmp = Bitmap.createBitmap(imgW, imgH, Bitmap.Config.ARGB_8888);
        if (RustBridge.fillBitmap(imageHandle, bmp) != 0) return;
        setBitmapOnly(bmp);
    }

    private void updateBitPlane() {
        if (imageHandle < 0) return;
        int channel = getSelectedChannel();
        int bit = getSelectedBit();
        byte[] gray = RustBridge.extractBitPlane(imageHandle, channel, bit);
        if (gray == null) return;
        int[] argb = new int[imgW * imgH];
        for (int i = 0; i < gray.length; i++) {
            int v = gray[i] & 0xff;
            argb[i] = 0xff000000 | (v << 16) | (v << 8) | v;
        }
        Bitmap bmp = Bitmap.createBitmap(argb, imgW, imgH, Bitmap.Config.ARGB_8888);
        setBitmapOnly(bmp);
    }

    @SuppressLint("SetTextI18n")
    private void setBitmapOnly(Bitmap bmp) {
        imageView.setImageBitmap(bmp);
        BitmapDrawable d = (BitmapDrawable) imageView.getDrawable();
        if (d != null) d.setFilterBitmap(false);
        statusText.setText(
                (int) (scale * 100) + "%  |  " + imgW + "x" + imgH + "  |  "
                        + "RGB".charAt(getSelectedChannel()) + " LSB-" + getSelectedBit());
    }

    private void fitToView() {
        int vw = imageView.getWidth();
        int vh = imageView.getHeight();
        if (vw == 0 || vh == 0 || imgW == 0 || imgH == 0) return;

        matrix.reset();
        float sx = (float) vw / imgW;
        float sy = (float) vh / imgH;
        scale = Math.min(sx, sy);
        minScale = scale;
        matrix.postScale(scale, scale);
        float tx = (vw - imgW * scale) / 2f;
        float ty = (vh - imgH * scale) / 2f;
        matrix.postTranslate(tx, ty);
        imageView.setImageMatrix(matrix);
    }

    private int getSelectedChannel() {
        int id = rgChannel.getCheckedRadioButtonId();
        if (id == R.id.ch_r) return 0;
        if (id == R.id.ch_g) return 1;
        return 2;
    }

    private int getSelectedBit() {
        int id = rgBit.getCheckedRadioButtonId();
        if (id == R.id.bit_0) return 0;
        if (id == R.id.bit_1) return 1;
        if (id == R.id.bit_2) return 2;
        if (id == R.id.bit_3) return 3;
        if (id == R.id.bit_4) return 4;
        if (id == R.id.bit_5) return 5;
        if (id == R.id.bit_6) return 6;
        return 7;
    }

    private byte[] readAllBytes(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) >= 0) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    @Override
    protected void onDestroy() {
        if (imageHandle >= 0) {
            RustBridge.freeImage(imageHandle);
            imageHandle = -1;
        }
        super.onDestroy();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float sf = detector.getScaleFactor();
            scale *= sf;
            if (scale < minScale * 0.5f) scale = minScale * 0.5f;
            if (scale > minScale * 10f) scale = minScale * 10f;
            matrix.postScale(sf, sf, detector.getFocusX(), detector.getFocusY());
            imageView.setImageMatrix(matrix);
            return true;
        }
    }
}
