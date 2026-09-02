package com.example.imusender;

public final class SensorState {
    private SensorState() {}
    public static volatile float ax, ay, az, gx, gy, gz;
    public static volatile boolean accelOk = false, gyroOk = false, running = false;
    public static volatile String host = "192.168.1.100";
    public static volatile int port = 5005;
    public static volatile long sentPackets = 0;
    public static volatile String lastError = "";
}
