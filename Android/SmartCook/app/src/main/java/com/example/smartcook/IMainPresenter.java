package com.example.smartcook;

public interface IMainPresenter {
    void StopSensors();
    void startBTConnection(String deviceAddress);
    void CheckBTAdapter();
    void CheckBTConnection();
    void UnRegisterReceiver();
}
