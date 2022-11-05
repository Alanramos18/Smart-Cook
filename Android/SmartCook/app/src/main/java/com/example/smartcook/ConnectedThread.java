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

    //Constructor de la clase del hilo secundario
    public ConnectedThread(BluetoothSocket socket) throws IOException {
        this.bluetoothIn = bluetoothIn;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try
        {
            //Create I/O streams for connection
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            throw e;
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run(int handlerState)
    {
        byte[] buffer = new byte[256];
        int bytes;

        while (true)
        {
            try
            {
                bytes = mmInStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);

                //se muestran en el layout de la activity, utilizando el handler del hilo
                // principal antes mencionado
                bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(String input) throws Exception {
        byte[] msgBuffer = input.getBytes();
        if(msgBuffer != null && msgBuffer.length > 0)
        {
            for(int i = 0; i < msgBuffer.length; i++)
            {
                Log.d("El buffer es: ", String.format("0x%20x", msgBuffer[i]));

            }
        }

        try {
            mmOutStream.write(msgBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
