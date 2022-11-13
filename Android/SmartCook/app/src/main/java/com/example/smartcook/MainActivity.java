package com.example.smartcook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button timeBtn;
    private Button activateBtn;
    private Button searchBtn;
    private Button doorBtn;
    private TextView stateText;
    private TextView connectedText;
    private TextView doorText;

    private IMainPresenter presenter;

    private boolean deviceConnected = false;
    private String deviceAddress;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        stateText = findViewById(R.id.stateText);
        connectedText = findViewById(R.id.connectedText);
        doorText = findViewById(R.id.doorText);
        timeBtn = findViewById(R.id.timeBtn);
        activateBtn = findViewById(R.id.activateBtn);
        searchBtn = findViewById(R.id.searchBtn);
        doorBtn = findViewById(R.id.doorBtn);

        timeBtn.setOnClickListener(timeListener);

        presenter = new MainPresenter(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        deviceAddress = extras.getString("Direccion_Bluetooth");

        if(deviceAddress != null && !deviceAddress.isEmpty())
        {
            presenter.startBTConnection(deviceAddress);
        }
    }

    @Override
    protected void onStop()
    {
        presenter.StopSensors();

        super.onStop();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {
        super.onPause();

        presenter.CheckBTAdapter();

        presenter.StopSensors();

        presenter.CheckBTConnection();
    }

    @Override
    public void onDestroy() {
        presenter.UnRegisterReceiver();
        presenter.StopSensors();

        super.onDestroy();
    }

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

    public ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
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

    public void showToast(String message) {
        if(message != null && !message.isEmpty())
        {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    // Setters

    public void setDoorText(String text, int color, boolean state) {
        doorText.setText(text);
        doorText.setTextColor(color);
        doorText.setEnabled(state);
    }

    public void setTimeBtn(boolean state) {
        timeBtn.setEnabled(state);
    }

    public void setDoorBtn(boolean state) {
        doorBtn.setEnabled(state);
    }

    public void setStateText(String text, int color) {
        stateText.setText(text);
        stateText.setTextColor(color);
    }

    public void setConnectedText(String text, int color) {
        stateText.setText(text);
        stateText.setTextColor(color);
    }

    public void setActivateBtn(String text, boolean state) {
        activateBtn.setText(text);
        activateBtn.setEnabled(state);
    }

    public void setSearchBtnText(boolean state) {
        searchBtn.setEnabled(state);
    }

    public void setSearchBtnListener(View.OnClickListener listener) {
        searchBtn.setOnClickListener(listener);
    }

    public void setActivateBtnListener(View.OnClickListener listener) {
        activateBtn.setOnClickListener(listener);
    }

    public void setDoorBtnListener(View.OnClickListener listener) {
        doorBtn.setOnClickListener(listener);
    }
}