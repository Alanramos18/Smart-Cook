package com.example.smartcook;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.hardware.SensorManager;
import android.os.Build;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.Context.SENSOR_SERVICE;

public class MainPresenter implements IMainPresenter, SensorEventListener {
    private MainActivity mainView;
    private boolean doorOpen;
    private boolean deviceConnected = false;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private ProgressDialog mProgressDlg;
    private BlueTooth blueTooth;

    private ConnectedThread mConnectedThread;

    private SensorManager sensorManager;

    private final static float ACC = 30;
    private final static String DOOR_ACTION = "-1";
    private final static String BLUETOOTH_CHECK = "0";
    public static final int MULTIPLE_PERMISSIONS = 10;

    String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MainPresenter(MainActivity mainView){
        this.mainView = mainView;

        blueTooth = BlueTooth.getInstance();

        initializeView();

        sensorManager = (SensorManager) mainView.getSystemService(SENSOR_SERVICE);
        InitializeDialog();
        InitializeSensors();

        if (checkPermissions()) {
            enableComponent();
        }
    }

    private void InitializeDialog() {
        mProgressDlg = new ProgressDialog(mainView);
        mProgressDlg.setMessage("Buscando dispositivos...");
        mProgressDlg.setCancelable(false);

        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", cancelDialogListener);
    }

    protected void enableComponent() {
        if (blueTooth.getBluetoothAdapter() == null) {
            showUnsupported();
        } else {
            mainView.setSearchBtnListener(searchListener);

            mainView.setActivateBtnListener(activateListener);

            if (blueTooth.getBluetoothAdapter().isEnabled()) {
                showEnabled();
            } else {
                showDisabled();
            }
        }

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mainView.registerReceiver(mReceiver, filter);
    }

    @SuppressLint("MissingPermission")
    public void startBTConnection(String deviceAddress)
    {
        try {
            blueTooth.StartConnection(deviceAddress);
        }
        catch (IOException e)
        {
            mainView.showToast( "La creación del Socket fallo");
        }

        try {
            mConnectedThread = new ConnectedThread(blueTooth.getBluetoothSocket());
            mConnectedThread.start();
            mConnectedThread.write(BLUETOOTH_CHECK);

            deviceConnected = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        StringBuilder text = new StringBuilder();
        text.append("Connected to: ");
        text.append(blueTooth.getDevice().getName());

        mainView.setConnectedText(text.toString(), Color.GREEN);
        mainView.setDoorBtn(true);
        mainView.setTimeBtn(true);
    }

    private void showUnsupported() {
        mainView.setStateText("Bluetooth no es soportado por el dispositivo movil", Color.RED);

        mainView.setActivateBtn("Activar Bluetooth", false);

        mainView.setSearchBtnText(false);
    }

    @SuppressLint("MissingPermission")
    public void CheckBTAdapter()
    {
        if (blueTooth.getBluetoothAdapter() != null) {
            if (blueTooth.getBluetoothAdapter().isDiscovering()) {
                blueTooth.getBluetoothAdapter().cancelDiscovery();
            }
        }
    }

    public void CheckBTConnection()
    {
        if(deviceConnected)
        {
            try {
                blueTooth.closeSocket();
            }
            catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    private void initializeView()
    {
        doorOpen = false;

        mainView.setTimeBtn(false);
        mainView.setDoorBtn(false);
        mainView.setDoorText("", Color.RED,false);

        mainView.setDoorBtnListener(doorListener);
    }

    private void CommandDoor() throws Exception {
        mConnectedThread.write(DOOR_ACTION);
        if(doorOpen)
        {
            doorOpen = false;
            mainView.showToast("Puerta Cerrada!");
            mainView.setDoorText("Puerta Cerrada!", Color.RED, true);
        }
        else
        {
            doorOpen = true;
            mainView.showToast("Puerta Abierta!");
            mainView.setDoorText("Puerta abierta!", Color.GREEN, true);
        }
    }

    private void showEnabled() {
        mainView.setStateText("Bluetooth Habilitado", Color.GREEN);

        mainView.setActivateBtn("Desactivar Bluetooth", true);

        mainView.setSearchBtnText(true);
    }

    private void showDisabled() {
        mainView.setStateText("Bluetooth Deshabilitado", Color.RED);

        mainView.setActivateBtn("Activar Bluetooth", true);

        mainView.setSearchBtnText(false);
    }

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(mainView,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(mainView, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    private void InitializeSensors()
    {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_NORMAL);
    }

    public void UnRegisterReceiver()
    {
        mainView.unregisterReceiver(mReceiver);
    }

    public void StopSensors()
    {
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON)
                {
                    mainView.showToast("Bluetooth Activado!");

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

                Intent newIntent = new Intent(mainView, DeviceListActivity.class);

                newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

                mainView.startActivity(newIntent);
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device != null && (device.getName() != null && !device.getName().isEmpty()) && !mDeviceList.contains(device))
                {
                    mDeviceList.add(device);

                    mainView.showToast("Dispositivo Encontrado:" + device.getName());
                }
            }
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Listeners
    private View.OnClickListener searchListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            blueTooth.getBluetoothAdapter().startDiscovery();
        }
    };

    private View.OnClickListener activateListener = new View.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View v) {
            if (blueTooth.getBluetoothAdapter().isEnabled()) {
                blueTooth.getBluetoothAdapter().disable();

                showDisabled();
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                mainView.someActivityResultLauncher.launch(intent);
            }
        }
    };

    private View.OnClickListener doorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(deviceConnected)
            {
                try {
                    CommandDoor();
                } catch (Exception e) {
                    mainView.showToast("La conexion fallo");
                    mainView.finish();
                }
            }
        }
    };

    private DialogInterface.OnClickListener cancelDialogListener = new DialogInterface.OnClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            blueTooth.getBluetoothAdapter().cancelDiscovery();
        }
    };
}
