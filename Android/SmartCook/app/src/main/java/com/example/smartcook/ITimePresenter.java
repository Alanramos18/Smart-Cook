package com.example.smartcook;

public interface ITimePresenter {
    void startBTConnection(String deviceAddress);
    void setTimeLeft(long time);
    long getTimeLeft();
    void setTimeRunning(boolean flag);
    void CheckBTConnection();
    void setFirstTime(boolean flag);
}
