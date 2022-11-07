package com.example.smartcook;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TimeActivity extends AppCompatActivity {

    private TextView temperatureText;
    private TextView pauseText;
    private TextView timerView;
    private Button firstBtn;
    private Button secondBtn;
    private Button timeBtn;
    private ConnectedThread mConnectedThread;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice device;
    private boolean firstTime = true;
    final int handlerState = 0;
    private StringBuilder recDataString = new StringBuilder();


    private CountDownTimer timer;
    private boolean timeRunning;
    private long timeLeft;

    private int minutes, seconds;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static String BLUETOOTH_CHECK = "0";
    private final static String PAUSE_ACTION = "-2";
    private final static String CANCEL_ACTION = "-3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);

        temperatureText = findViewById(R.id.temperatureText);
        timerView = findViewById(R.id.timer);
        pauseText = findViewById(R.id.pauseText);
        firstBtn = findViewById(R.id.firstBtn);
        secondBtn = findViewById(R.id.secondBtn);
        timeBtn = findViewById(R.id.timeBtn);

        temperatureText.setText("0");
        firstBtn.setText("Iniciar");
        secondBtn.setText("Volver");
        timeBtn.setText("Elegir tiempo");

        timerView.setVisibility(View.INVISIBLE);
        pauseText.setVisibility(View.INVISIBLE);

        firstBtn.setOnClickListener(firstBtnListener);
        secondBtn.setOnClickListener(secondBtnListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private View.OnClickListener secondBtnListener = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View view) {
            if(timeRunning)
            {
                try {
                    mConnectedThread.write(CANCEL_ACTION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Intent intent = new Intent(TimeActivity.this, MainActivity.class);
            intent.putExtra("Direccion_Bluetooth", device.getAddress());

            startActivity(intent);

            finish();
        }
    };

    private View.OnClickListener firstBtnListener = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View view) {
            if(timeRunning)
            {
                try {
                    pauseTimer();
                    pauseText.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                if(minutes == 0 && seconds == 0)
                {
                    showToast("Por favor, ingrese un tiempo");
                    return;
                }

                try {
                    if(firstTime)
                    {
                        mConnectedThread.write(Integer.toString(seconds));

                        timeLeft = TimeUnit.SECONDS.toMillis(seconds);

                        firstTime = false;
                    }
                    else
                    {
                        mConnectedThread.write(PAUSE_ACTION);
                        pauseText.setVisibility(View.INVISIBLE);
                    }

                    StartTimer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void popTimePicker(View view) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedMinutes, int selectedSeconds) {
                minutes = selectedMinutes;
                seconds = selectedSeconds;
                timeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }
        };

        int style = AlertDialog.THEME_HOLO_DARK;

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, style, onTimeSetListener, minutes, seconds, true);

        timePickerDialog.setTitle("Seleccione el tiempo de cocción");
        timePickerDialog.show();
    }

    private void pauseTimer() throws Exception {
        mConnectedThread.write(PAUSE_ACTION);
        timer.cancel();
        timeRunning = false;
        firstBtn.setText("Continuar");
    }

    private void StartTimer()
    {
        timer = new CountDownTimer(timeLeft, 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                timeLeft = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish()
            {
                timeRunning = false;
                timerView.setVisibility(View.INVISIBLE);
                timeBtn.setVisibility(View.VISIBLE);
                timeBtn.setText("Listo!");
            }
        }.start();

        timeRunning = true;
        firstBtn.setText("Pausar");
        secondBtn.setText("Cancelar");
        timeBtn.setVisibility(View.INVISIBLE);
        timerView.setVisibility(View.VISIBLE);
    }

    private void updateCountDownText()
    {
        int minutes = (int) (timeLeft / 1000) / 60;
        int seconds = (int) (timeLeft / 1000) % 60;

        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        timerView.setText(timeLeftFormatted);
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

    @SuppressLint("MissingPermission")
    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        String deviceAddress = extras.getString("Direccion_Bluetooth");

        if(deviceAddress != null && !deviceAddress.isEmpty())
        {
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
                mConnectedThread.setBluetoothIn(HandlerMsgHiloPrincipal(), handlerState);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private Handler HandlerMsgHiloPrincipal()
    {
        return new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg)
            {
                if(msg.what == handlerState)
                {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r\n");

                    if(endOfLineIndex > 0)
                    {
                        String temperature = recDataString.substring(0, endOfLineIndex);
                        setTemperature(temperature);

                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    private void setTemperature(String temp)
    {
        int temperature = Integer.parseInt(temp) / 10;

        temperatureText.setText(Integer.toString(temperature) + "\u2103");
    }
}