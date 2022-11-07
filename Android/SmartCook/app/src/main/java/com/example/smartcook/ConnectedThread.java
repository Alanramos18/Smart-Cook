package com.example.smartcook;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread{

    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler bluetoothIn;
    private int handlerState;


    public ConnectedThread(BluetoothSocket socket) throws IOException {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try
        {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            throw e;
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run()
    {
        byte[] buffer = new byte[256];
        int bytes;

        while (true)
        {
            try
            {
                bytes = mmInStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);

                if(bluetoothIn != null)
                {
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(String input) throws Exception {
        byte[] msgBuffer = input.getBytes();

        try {
            mmOutStream.write(msgBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBluetoothIn(Handler bluetoothIn, int handlerState) {
        this.bluetoothIn = bluetoothIn;
        this.handlerState = handlerState;
    }
}
