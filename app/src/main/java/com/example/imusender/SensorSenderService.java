package com.example.imusender;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;

public class SensorSenderService extends Service implements SensorEventListener {
    public static final String ACTION_START = "com.example.imusender.START";
    public static final String ACTION_STOP = "com.example.imusender.STOP";
    public static final String EXTRA_HOST = "host";
    public static final String EXTRA_PORT = "port";
    private static final String CHANNEL = "imu_sender";
    private static final int NOTIFY_ID = 5005;

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private DatagramSocket socket;
    private InetAddress address;
    private ScheduledExecutorService sender;
    private PowerManager.WakeLock wakeLock;

    @Override public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SensorState.accelOk = accelerometer != null;
        SensorState.gyroOk = gyroscope != null;
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSending();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            SensorState.host = intent.getStringExtra(EXTRA_HOST) == null ? SensorState.host : intent.getStringExtra(EXTRA_HOST);
            SensorState.port = intent.getIntExtra(EXTRA_PORT, SensorState.port);
        }
        startForeground(NOTIFY_ID, notification("发送到 " + SensorState.host + ":" + SensorState.port));
        startSending();
        return START_STICKY;
    }

    private synchronized void startSending() {
        stopWorkersOnly();
        try {
            address = InetAddress.getByName(SensorState.host);
            socket = new DatagramSocket();
            socket.setBroadcast(false);
            if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            if (gyroscope != null) sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IMUSender:SensorWakeLock");
            wakeLock.acquire(6 * 60 * 60 * 1000L);
            sender = Executors.newSingleThreadScheduledExecutor();
            sender.scheduleAtFixedRate(new Runnable() {
                @Override public void run() { sendPacket(); }
            }, 0, 20, TimeUnit.MILLISECONDS); // 50 Hz
            SensorState.running = true;
            SensorState.lastError = "";
        } catch (Exception e) {
            SensorState.lastError = e.getMessage() == null ? e.toString() : e.getMessage();
            SensorState.running = false;
        }
    }

    private void sendPacket() {
        try {
            long ts = System.currentTimeMillis();
            String json = String.format(Locale.US,
                "{\"timestamp\":%d,\"ax\":%.6f,\"ay\":%.6f,\"az\":%.6f,\"gx\":%.6f,\"gy\":%.6f,\"gz\":%.6f}\n",
                ts, SensorState.ax, SensorState.ay, SensorState.az,
                SensorState.gx, SensorState.gy, SensorState.gz);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, address, SensorState.port));
            SensorState.sentPackets++;
        } catch (Exception e) {
            SensorState.lastError = e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            SensorState.ax = event.values[0]; SensorState.ay = event.values[1]; SensorState.az = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            SensorState.gx = event.values[0]; SensorState.gy = event.values[1]; SensorState.gz = event.values[2];
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "IMU 后台发送", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("保持加速度计和陀螺仪数据在后台发送");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("IMU Sender 正在运行")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void stopWorkersOnly() {
        if (sender != null) { sender.shutdownNow(); sender = null; }
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (socket != null) { socket.close(); socket = null; }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }

    private void stopSending() {
        stopWorkersOnly();
        SensorState.running = false;
        stopForeground(true);
    }

    @Override public void onDestroy() { stopSending(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
