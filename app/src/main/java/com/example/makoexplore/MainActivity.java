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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnTouchListener {
    private static final String TAG = "Touch";
    private static final int REQUEST_ENABLE_BT = 1;

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

    // Bluetooth
    BluetoothAdapter bluetoothAdapter;
    ArrayList<String> pairedDeviceArrayList;

    ListView listViewPairedDevice;
    FrameLayout ButPanel;

    ArrayAdapter<String> pairedDeviceAdapter;

    ThreadConnectBTdevice myThreadConnectBTdevice;


    private StringBuilder sb = new StringBuilder();

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

        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
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

    private void setup() { // Создание списка сопряжённых Bluetooth-устройств

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства

            pairedDeviceArrayList = new ArrayList<>();

            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }

            pairedDeviceAdapter = new ArrayAdapter<>(this, R.layout.activity_main, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    listViewPairedDevice.setVisibility(View.GONE); // После клика скрываем список

                    String  itemValue = (String) listViewPairedDevice.getItemAtPosition(position);
                    String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес

                    BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(MAC);

                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
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