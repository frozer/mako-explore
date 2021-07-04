package com.example.makoexplore;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
    ThreadConnected myThreadConnected;

    private UUID myUUID;
    private BluetoothSocket bluetoothSocket = null;

    public ThreadConnectBTdevice(BluetoothDevice device) {

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() { // Коннект

        boolean success = false;

        try {
            bluetoothSocket.connect();
            success = true;
        }

        catch (IOException e) {
            e.printStackTrace();

            try {
                bluetoothSocket.close();
            }

            catch (IOException e1) {

                e1.printStackTrace();
            }
        }

        if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных


            myThreadConnected = new ThreadConnected(bluetoothSocket);
            myThreadConnected.start(); // запуск потока приёма и отправки данных
        }
    }


    public void cancel() {

        try {
            bluetoothSocket.close();
        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
