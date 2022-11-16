package com.example.smartcook;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

public class BlueTooth {

    private static BlueTooth blueTooth;
    private static BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice device;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BlueTooth() {}

    public static BlueTooth getInstance()
    {
        if (blueTooth==null)
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            blueTooth = new BlueTooth();
        }
        return blueTooth;
    }

    @SuppressLint("MissingPermission")
    public void StartConnection(String deviceAddress) throws IOException {

        if(deviceAddress != null && !deviceAddress.isEmpty()) {
            device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                throw e;
            }

            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }

            }
        }
    }

    public BluetoothAdapter getBluetoothAdapter()
    {
        return mBluetoothAdapter;
    }

    public BluetoothSocket getBluetoothSocket()
    {
        return btSocket;
    }

    public BluetoothDevice getDevice()
    {
        return device;
    }

    public void closeSocket() throws IOException {
        btSocket.close();
    }
    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }
}
