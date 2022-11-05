package com.example.smartcook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button timeBtn;
    private Button activateBtn;
    private Button searchBtn;
    private Button doorBtn;
    private TextView stateText;
    private TextView connectedText;
    private TextView doorText;
    private ProgressDialog mProgressDlg;
    private ConnectedThread mConnectedThread;
    private boolean doorOpen;
    private boolean appRunning = false;

    private boolean deviceConnected = false;
    private BluetoothDevice device;
    private String deviceAddress;

    private SensorManager sensorManager;
    private final static float ACC = 30;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket;

    public static final int MULTIPLE_PERMISSIONS = 10;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static String DOOR_ACTION = "-1";
    private final static String BLUETOOTH_CHECK = "0";

    String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doorOpen = false;

        stateText = findViewById(R.id.stateText);
        connectedText = findViewById(R.id.connectedText);
        doorText = findViewById(R.id.doorText);
        timeBtn = findViewById(R.id.timeBtn);
        activateBtn = findViewById(R.id.activateBtn);
        searchBtn = findViewById(R.id.searchBtn);
        doorBtn = findViewById(R.id.doorBtn);

        timeBtn.setOnClickListener(timeListener);
        doorBtn.setOnClickListener(doorListener);

        //timeBtn.setEnabled(true);
        timeBtn.setEnabled(false);
        doorBtn.setEnabled(false);
        doorText.setEnabled(false);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setMessage("Buscando dispositivos...");
        mProgressDlg.setCancelable(false);

        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", cancelDialogListener);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        InitializeSensors();

        if (checkPermissions()) {
            enableComponent();
        }
    }

    protected void enableComponent() {
        if (mBluetoothAdapter == null) {
            showUnsupported();
        } else {
            searchBtn.setOnClickListener(searchListener);

            activateBtn.setOnClickListener(activateListener);

            if (mBluetoothAdapter.isEnabled()) {
                showEnabled();
            } else {
                showDisabled();
            }
        }

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //Cambia el estado del Bluethoot (Acrtivado /Desactivado)
        filter.addAction(BluetoothDevice.ACTION_FOUND); //Se encuentra un dispositivo bluethoot al realizar una busqueda
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Cuando se comienza una busqueda de bluethoot
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //cuando la busqueda de bluethoot finaliza

        registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON)
                {
                    showToast("Bluetooth Activado!");

                    showEnabled();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                mDeviceList = new ArrayList<BluetoothDevice>();

                mProgressDlg.show();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                mProgressDlg.dismiss();

                Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);

                newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

                startActivity(newIntent);
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(!mDeviceList.contains(device))
                {
                    mDeviceList.add(device);

                    showToast("Dispositivo Encontrado:" + device.getName());
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        deviceAddress = extras.getString("Direccion_Bluetooth");

        if(deviceAddress != null && !deviceAddress.isEmpty())
        {
            deviceConnected = true;

            device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

            try {
                btSocket = createBluetoothSocket(device);
            }
            catch (IOException e)
            {
                showToast( "La creación del Socket fallo");
            }

            try
            {
                btSocket.connect();
            }

            catch (IOException e)
            {
                try
                {
                    btSocket.close();
                }
                catch (IOException e2)
                {
                    e2.printStackTrace();
                }

            }

            try {
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
                mConnectedThread.write(BLUETOOTH_CHECK);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            StringBuilder text = new StringBuilder();
            text.append("Connected to: ");
            text.append(device.getName());

            connectedText.setText(text.toString());
            doorBtn.setEnabled(true);
            timeBtn.setEnabled(true);
        }
    }

    @Override
    protected void onStop()
    {
        StopSensors();

        super.onStop();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
        StopSensors();

        super.onPause();

        if(deviceConnected)
        {
            try {
                btSocket.close();
            }
            catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        StopSensors();

        super.onDestroy();
    }

    private View.OnClickListener searchListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            mBluetoothAdapter.startDiscovery();
        }
    };

    private View.OnClickListener activateListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();

                showDisabled();
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                someActivityResultLauncher.launch(intent);
            }
        }
    };

    private View.OnClickListener timeListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            if(deviceConnected)
            {
                Intent newIntent = new Intent(MainActivity.this, TimeActivity.class);
                newIntent.putExtra("Direccion_Bluetooth", deviceAddress);
                startActivity(newIntent);
            }
        }
    };

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                    }
                }
            }
    );

    private View.OnClickListener doorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(deviceConnected)
            {
                try {
                    CommandDoor();
                } catch (Exception e) {
                    showToast("La conexion fallo");
                    finish();
                }
            }
        }
    };

    private DialogInterface.OnClickListener cancelDialogListener = new DialogInterface.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            mBluetoothAdapter.cancelDiscovery();
        }
    };

    private void CommandDoor() throws Exception {
        mConnectedThread.write(DOOR_ACTION);
        if(doorOpen)
        {
            doorOpen = false;
            showToast("Puerta Cerrada!");
            doorText.setEnabled(true);
            doorText.setText("Puerta Cerrada!");
            doorText.setTextColor(Color.RED);
        }
        else
        {
            doorOpen = true;
            showToast("Puerta Abierta!");
            doorText.setEnabled(true);
            doorText.setText("Puerta abierta!");
            doorText.setTextColor(Color.GREEN);
        }
    }

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    private void showEnabled() {
        stateText.setText("Bluetooth Habilitado");
        stateText.setTextColor(Color.GREEN);

        activateBtn.setText("Desactivar Bluetooth");
        activateBtn.setEnabled(true);

        searchBtn.setEnabled(true);
    }

    private void showDisabled() {
        stateText.setText("Bluetooth Deshabilitado");
        stateText.setTextColor(Color.RED);

        activateBtn.setText("Activar Bluetooth");
        activateBtn.setEnabled(true);

        searchBtn.setEnabled(false);
    }

    private void showUnsupported() {
        stateText.setText("Bluetooth no es soportado por el dispositivo movil");

        activateBtn.setText("Activar Bluetooth");
        activateBtn.setEnabled(false);

        searchBtn.setEnabled(false);
    }

    private void showToast(String message) {
        if(message != null && !message.isEmpty())
        {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!deviceConnected)
        {
            return;
        }

        int sensorType = sensorEvent.sensor.getType();

        float[] values = sensorEvent.values;

        if (sensorType == Sensor.TYPE_ACCELEROMETER)
        {
            if ((Math.abs(values[0]) > ACC || Math.abs(values[1]) > ACC || Math.abs(values[2]) > ACC))
            {
                try {
                    CommandDoor();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void InitializeSensors()
    {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_NORMAL);
    }

    private void StopSensors()
    {
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }
}