package com.example.makoexplore;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

public class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
    ThreadConnected myThreadConnected;

    private UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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

            myThreadConnected = null;
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
