package com.example.makoexplore;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.widget.ImageViewCompat;

public class MainActivity extends Activity implements View.OnTouchListener {
    private static final String TAG = "Touch";

    // These matrices will be used to move image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    Matrix initialMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    PointF start = new PointF();

    // limit movement with this value
    float maxRadius;
    float centerX;
    float centerY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ImageView view = (ImageView) findViewById(R.id.imageView);
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        centerImage(view);
                    }
                }
        );

        view.setOnTouchListener(this);

    }

    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;

        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: //first finger down only

                savedMatrix.set(initialMatrix);
                matrix.set(initialMatrix);

                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG; " + event.getX() + "; " + event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: //first finger lifted
                mode = NONE;
                Log.d(TAG, "mode=NONE" );

                matrix.set(initialMatrix);

                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG)
                { //movement of first finger
                    float x = event.getX();
                    float y = event.getY();

                    matrix.set(savedMatrix);

                    if (isFit(x, y))
                    {
                        matrix.postTranslate(x - start.x, y - start.y);

                        drive(x, y);
                    }
                }
                break;
        }

        // Perform the transformation
        view.setImageMatrix(matrix);

        return true; // indicate event was handled
    }

    private void centerImage(ImageView imageView) {
        Drawable d = imageView.getDrawable();
        initialMatrix = new Matrix();

        if (d != null) {
            float offsetX = (imageView.getWidth() - d.getIntrinsicWidth()) / 2F;
            float offsetY = (imageView.getHeight() - d.getIntrinsicHeight()) / 2F;

            centerX = imageView.getWidth() / 2F;
            centerY = imageView.getHeight() / 2F;
            maxRadius = centerX > centerY ? centerY : centerX;

            initialMatrix.setTranslate(offsetX, offsetY);

            imageView.setImageMatrix(initialMatrix);
        }
    }

    private boolean isFit(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return Math.sqrt(Math.pow(dx,2) + Math.pow(dy, 2)) <= maxRadius;
    }
//    private void resizeWorkArea() {
//        ImageView workArea = (ImageView) findViewById(R.id.workArea);
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(maxRadius, maxRadius);
//
//        workArea.setLayoutParams(params);
//    }

    private void drive(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;

        float accelX = Math.round(dx * 255 / maxRadius);
        float accelY = Math.round(dy * 255 / maxRadius);

        Log.d(TAG, "Accel X = " + accelX + ", Accel Y = " + accelY);
    }
}