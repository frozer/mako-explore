package com.example.makoexplore;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends Activity implements View.OnTouchListener {
    private static final String TAG = "Touch";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final double SCALE_FACTOR = 1.45;

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
    float correctionY;
    float correctionX;
    float maxRadius;
    float centerX;
    float centerY;

    // Bluetooth
    BluetoothAdapter bluetoothAdapter;
    ArrayList<String> pairedDeviceArrayList;

    FrameLayout ButPanel;

    ArrayAdapter<String> pairedDeviceAdapter;

    ThreadConnectBTdevice myThreadConnectBTdevice;

    TextView leftMotorSpeedLabel;
    TextView rightMotorSpeedLabel;

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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Toast.makeText(this, bluetoothAdapter.getName() + " " + bluetoothAdapter.getAddress(), Toast.LENGTH_LONG).show();

        leftMotorSpeedLabel = (TextView) findViewById(R.id.leftSpeedLabel);
        rightMotorSpeedLabel = (TextView) findViewById(R.id.rightSpeedLabel);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    /**
     * Connect to paired BT device
     */
    private void setup() {

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                BluetoothDevice dev = bluetoothAdapter.getRemoteDevice(device.getAddress());

                myThreadConnectBTdevice = new ThreadConnectBTdevice(dev);
                myThreadConnectBTdevice.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
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

                drive(0, 0);

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
            maxRadius = Math.min(centerX, centerY);

            initialMatrix.setTranslate(offsetX, offsetY);

            imageView.setImageMatrix(initialMatrix);

            Log.d(TAG, "center X = " + centerX + ", center Y = " + centerY + ", max radius = " + maxRadius);
        }
    }

    private boolean isFit(float x, float y) {
        int angle = -135;
        double rad = angle * Math.PI / 180;

        double newX = (x - centerX) * Math.cos(rad) - (centerY -y) * Math.sin(rad);
        double newY = (centerY - y) * Math.cos(rad) + (x - centerX) * Math.sin(rad);
        return Math.sqrt(Math.pow(newX,2) + Math.pow(newY, 2)) <= maxRadius;
    }

    private void drive(float x, float y) {

        Log.d(TAG, "x = " + x + ", y = " + y);

        double[] speeds = mode == DRAG ? convertCoordToSpeed(x, y) : new double[2];
        String command = getCommand(speeds[0], speeds[1]);

        Log.d(TAG, "LMS = " + speeds[0] + ", RMS = " + speeds[1]);

        leftMotorSpeedLabel.setText("LMS: " + speeds[0]);
        rightMotorSpeedLabel.setText("RMS: " + speeds[1]);

        if (myThreadConnectBTdevice.myThreadConnected !=  null) {
            myThreadConnectBTdevice.myThreadConnected.write((command).getBytes());
        }
    }

    // https://math.stackexchange.com/questions/553478/how-covert-joysitck-x-y-coordinates-to-robot-motor-speed
    private double[] convertCoordToSpeed(double x, double y) {
        double[] res = new double[2];
// Option 1.
        int angle = -135;
        double rad = angle * Math.PI / 180;

        double newX = (x - centerX) * Math.cos(rad) - (centerY -y) * Math.sin(rad);
        double newY = (centerY - y) * Math.cos(rad) + (x - centerX) * Math.sin(rad);
        // angle between vector and X-axis
        double theta = Math.atan2(newY, newX);
        //
        double magnitude = Math.sqrt(Math.pow(newX, 2) + Math.pow(newY, 2));

        double xCoord = magnitude * Math.cos(theta);
        double yCoord = magnitude * Math.sin(theta);

        double rightMotorSpeed = Math.round(SCALE_FACTOR * Math.round(xCoord * 255 / maxRadius));
        double leftMotorSpeed = Math.round(SCALE_FACTOR * -1 * Math.round( yCoord * 255 / maxRadius));

        res[0] = leftMotorSpeed;
        res[1] = rightMotorSpeed;

        return res;
    }

    private String getCommand(double left, double right) {
        String command = left + ":" + right;
        // removing the ending .0, to avoid collisions on Arduino side
        return command.replace(".0", "") + ";";
    }
}