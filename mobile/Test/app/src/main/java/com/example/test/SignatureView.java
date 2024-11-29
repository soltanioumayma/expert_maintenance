package com.example.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {
    private Paint paint;
    private Path path;
    private Bitmap bitmap; // For off-screen drawing
    private Canvas bitmapCanvas; // Canvas to draw onto the bitmap
    private boolean hasSignature = false; // To track if the user has drawn something

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize the paint object
        paint = new Paint();
        paint.setColor(Color.BLACK); // Pen color
        paint.setStyle(Paint.Style.STROKE); // Stroke style
        paint.setStrokeWidth(5f); // Pen thickness
        paint.setAntiAlias(true); // Smooth edges
        paint.setStrokeCap(Paint.Cap.ROUND); // Rounded stroke ends

        // Initialize the path for drawing
        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Create a bitmap for off-screen drawing
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawColor(Color.WHITE); // Set background to white
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the bitmap first (previous strokes)
        canvas.drawBitmap(bitmap, 0, 0, null);

        // Draw the current path on top
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Capture user touch events
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y); // Start a new path
                invalidate();
                hasSignature = true; // Track that a signature has been made
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y); // Draw a line to the current position
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                bitmapCanvas.drawPath(path, paint); // Draw the path onto the bitmap
                path.reset(); // Clear the current path for the next stroke
                invalidate();
                break;
        }
        return true; // Consume the touch event
    }

    public void clearSignature() {
        // Clear the path and bitmap to erase the signature
        path.reset();
        bitmap.eraseColor(Color.WHITE); // Reset the bitmap to white
        hasSignature = false; // Reset the signature flag
        invalidate();
    }

    public Bitmap getSignatureBitmap() {
        if (!hasSignature) {
            return null; // Return null if no signature has been drawn
        }
        return bitmap;
    }

    public boolean hasSignature() {
        return hasSignature;
    }
}
