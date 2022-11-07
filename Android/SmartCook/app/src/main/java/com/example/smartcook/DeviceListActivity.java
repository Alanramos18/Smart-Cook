package com.example.smartcook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class DeviceListActivity extends Activity {

    private ListView mListView;
    private DeviceListAdapter mAdapter;
    private ArrayList<BluetoothDevice> mDeviceList;
    private int posicionListBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        mListView = (ListView) findViewById(R.id.lv_paired);

        mDeviceList = getIntent().getExtras().getParcelableArrayList("device.list");

        mAdapter = new DeviceListAdapter(this);

        mAdapter.setData(mDeviceList);

        mAdapter.setListener(listenerBotonEmparejar);
        mAdapter.setSelectListener(listenerSelector);
        mListView.setAdapter(mAdapter);

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mPairReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPairReceiver);

        super.onDestroy();
    }

    private void showToast(String message) {
        if(message != null && !message.isEmpty())
        {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DeviceListAdapter.OnPairButtonClickListener listenerBotonEmparejar = new DeviceListAdapter.OnPairButtonClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onPairButtonClick(int position) {
            BluetoothDevice device = mDeviceList.get(position);

            if (device.getBondState() == BluetoothDevice.BOND_BONDED)
            {
                unpairDevice(device);
            }
            else
            {
                showToast("Emparejando");
                posicionListBluetooth = position;
                pairDevice(device);
            }
        }
    };

    private DeviceListAdapter.OnSelectorClickListener listenerSelector = new DeviceListAdapter.OnSelectorClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onSelectorClick(String address) {
            Intent i = new Intent(DeviceListActivity.this, MainActivity.class);
            i.putExtra("Direccion_Bluetooth", address);

            startActivity(i);
        }
    };

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING)
                {
                    BluetoothDevice dispositivo = (BluetoothDevice) mAdapter.getItem(posicionListBluetooth);

                    showToast(dispositivo.getName());
                    String direccionBluetooth = dispositivo.getAddress();
                    Intent i = new Intent(DeviceListActivity.this, MainActivity.class);
                    i.putExtra("Direccion_Bluetooth", direccionBluetooth);

                    startActivity(i);
                }
                else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    showToast("No emparejado");
                }

                mAdapter.notifyDataSetChanged();
            }
        }
    };
}