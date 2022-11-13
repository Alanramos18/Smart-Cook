package com.example.smartcook;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.TimePicker;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimePresenter implements ITimePresenter{

    private TimeActivity timeView;
    private CountDownTimer timer;

    private BlueTooth blueTooth;
    private ConnectedThread mConnectedThread;

    private boolean firstTime = true;
    private boolean timeRunning;
    private long timeLeft;
    private int minutes, seconds;

    final int handlerState = 0;
    private StringBuilder recDataString = new StringBuilder();


    private final static String BLUETOOTH_CHECK = "0";
    private final static String PAUSE_ACTION = "-2";
    private final static String CANCEL_ACTION = "-3";

    public TimePresenter(TimeActivity timeView){
        this.timeView = timeView;

        blueTooth = BlueTooth.getInstance();

        InitializeView();
    }

    private void InitializeView() {
        timeView.setTemperatureText("0");
        timeView.setFirstBtn("Iniciar");
        timeView.setSecondBtn("Volver");
        timeView.setTimeBtn("Elegir tiempo");

        timeView.setTimerViewVisibility(View.INVISIBLE);
        timeView.setPauseTextVisibility(View.INVISIBLE);

        timeView.setFirstBtnListener(firstBtnListener);
        timeView.setSecondBtnListener(secondBtnListener);
    }

    public void popTimePicker(View view) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedMinutes, int selectedSeconds) {
                minutes = selectedMinutes;
                seconds = selectedSeconds;
                timeView.setTimeBtn(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }
        };

        int style = AlertDialog.THEME_HOLO_DARK;

        TimePickerDialog timePickerDialog = new TimePickerDialog(timeView, style, onTimeSetListener, minutes, seconds, true);

        timePickerDialog.setTitle("Seleccione el tiempo de cocción");
        timePickerDialog.show();
    }

    @SuppressLint("MissingPermission")
    public void startBTConnection(String deviceAddress)
    {
        try {
            blueTooth.StartConnection(deviceAddress);
        }
        catch (IOException e)
        {
            timeView.showToast( "La creación del Socket fallo");
        }

        try
        {
            blueTooth.getBluetoothSocket().connect();
        }

        catch (IOException e)
        {
            try
            {
                blueTooth.getBluetoothSocket().close();
            }
            catch (IOException e2)
            {
                e2.printStackTrace();
            }
        }

        try {
            mConnectedThread = new ConnectedThread(blueTooth.getBluetoothSocket());
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

    private void pauseTimer() throws Exception {
        mConnectedThread.write(PAUSE_ACTION);
        timer.cancel();
        timeRunning = false;
        timeView.setFirstBtn("Continuar");
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
                timeView.setTimerViewVisibility(View.INVISIBLE);
                timeView.setTimeBtnVisibility(View.VISIBLE);
                timeView.setTimeBtn("Listo!");
            }
        }.start();

        timeRunning = true;
        timeView.setFirstBtn("Pausar");
        timeView.setSecondBtn("Cancelar");

        timeView.setTimerViewVisibility(View.VISIBLE);
        timeView.setTimeBtnVisibility(View.INVISIBLE);
    }

    private void updateCountDownText()
    {
        int minutes = (int) (timeLeft / 1000) / 60;
        int seconds = (int) (timeLeft / 1000) % 60;

        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        timeView.setTimerView(timeLeftFormatted);
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

        timeView.setTemperatureText(Integer.toString(temperature) + "\u2103");
    }

    // Listeners

    private View.OnClickListener firstBtnListener = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View view) {
            if(timeRunning)
            {
                try {
                    pauseTimer();
                    timeView.setPauseTextVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                if(minutes == 0 && seconds == 0)
                {
                    timeView.showToast("Por favor, ingrese un tiempo");
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
                        timeView.setPauseTextVisibility(View.INVISIBLE);
                    }

                    StartTimer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

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

            Intent intent = new Intent(timeView, MainActivity.class);
            intent.putExtra("Direccion_Bluetooth", blueTooth.getDevice().getAddress());

            timeView.startActivity(intent);

            timeView.finish();
        }
    };
}
