package com.example.makoexplore;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ThreadConnected extends Thread {    // Поток - приём и отправка данных

    private final InputStream connectedInputStream;
    private final OutputStream connectedOutputStream;

    private StringBuilder sb = new StringBuilder();
    private String sbprint;

    public ThreadConnected(BluetoothSocket socket) {

        InputStream in = null;
        OutputStream out = null;

        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        connectedInputStream = in;
        connectedOutputStream = out;
    }


    @Override
    public void run() { // Приём данных

        while (true) {
            try {
                byte[] buffer = new byte[1];
                int bytes = connectedInputStream.read(buffer);
                String strIncom = new String(buffer, 0, bytes);
                sb.append(strIncom); // собираем символы в строку
                int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки

                if (endOfLineIndex > 0) {

                    sbprint = sb.substring(0, endOfLineIndex);
                    sb.delete(0, sb.length());
                }
            } catch (IOException e) {
                break;
            }
        }
    }


    public void write(byte[] buffer) {
        try {
            connectedOutputStream.write(buffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
