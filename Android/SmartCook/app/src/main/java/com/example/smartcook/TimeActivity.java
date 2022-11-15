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

    private ITimePresenter presenter;

    private TextView temperatureText;
    private TextView pauseText;
    private TextView timerView;
    private Button firstBtn;
    private Button secondBtn;
    private Button timeBtn;
    private CountDownTimer timer;
    private int minutes, seconds;

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

        presenter = new TimePresenter(this);
    }

    public void showToast(String message) {
        if(message != null && !message.isEmpty())
        {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        String deviceAddress = extras.getString("Direccion_Bluetooth");

        if(deviceAddress != null && !deviceAddress.isEmpty())
        {
            presenter.startBTConnection(deviceAddress);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {
        super.onPause();

        presenter.CheckBTConnection();
    }

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

    // Setters
    public void setTemperatureText(String text)
    {
        temperatureText.setText(text);
    }

    public void setFirstBtn(String text)
    {
        firstBtn.setText(text);

    }

    public void setSecondBtn(String text)
    {
        secondBtn.setText(text);

    }

    public void setTimeBtn(String text)
    {
        timeBtn.setText(text);
    }

    public void setTimerViewVisibility(int visibility)
    {
        timerView.setVisibility(visibility);
    }

    public void setTimeBtnVisibility(int visibility)
    {
        timeBtn.setVisibility(visibility);
    }

    public void setPauseTextVisibility(int visibility)
    {
        pauseText.setVisibility(visibility);
    }

    public void setFirstBtnListener(View.OnClickListener listener)
    {
        firstBtn.setOnClickListener(listener);
    }

    public void setSecondBtnListener(View.OnClickListener listener)
    {
        secondBtn.setOnClickListener(listener);
    }

    public void cancelTimer()
    {
        timer.cancel();
    }

    public void StartTimer() {
        timer = new CountDownTimer(presenter.getTimeLeft(), 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                presenter.setTimeLeft(millisUntilFinished);
                updateCountDownText();
            }

            @Override
            public void onFinish()
            {
                presenter.setTimeRunning(false);
                presenter.setFirstTime(true);
                timerView.setVisibility(View.INVISIBLE);
                timeBtn.setVisibility(View.VISIBLE);
                timeBtn.setText("Elegir tiempo");
                firstBtn.setText("Iniciar");
                secondBtn.setText("Volver");
                showToast("Finalizado!");
            }
        }.start();
    }

    private void updateCountDownText()
    {
        long timeLeft = presenter.getTimeLeft();
        int minutes = (int) (timeLeft / 1000) / 60;
        int seconds = (int) (timeLeft / 1000) % 60;

        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        timerView.setText(timeLeftFormatted);
    }

    public int getMinutes()
    {
        return minutes;
    }

    public int getSeconds()
    {
        return seconds;
    }
}