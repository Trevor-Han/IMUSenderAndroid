package com.example.imusender;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText hostInput, portInput;
    private TextView destination, sensorStatus, values, packetCount, errorText;
    private Switch backgroundSwitch;
    private Button actionButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updater = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 100); }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(247,248,252));
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 33);
        }
        setContentView(buildUi());
        handler.post(updater);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247,248,252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(26));
        scroll.addView(root, new ScrollView.LayoutParams(-1,-1));

        TextView title = text("IMU Sender", 34, Color.rgb(22,26,36), Typeface.NORMAL);
        root.addView(title);
        TextView subtitle = text("通过 Wi‑Fi 把手机加速度计和陀螺仪数据发到电脑", 16, Color.rgb(63,69,82), Typeface.NORMAL);
        LinearLayout.LayoutParams subLp = lp(-1,-2); subLp.topMargin=dp(12); subLp.bottomMargin=dp(18); root.addView(subtitle, subLp);

        LinearLayout card = card();
        root.addView(card, lp(-1,-2));
        hostInput = input("电脑 IP", "192.168.1.100", InputType.TYPE_CLASS_PHONE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        card.addView(hostInput, lp(-1, dp(76)));
        LinearLayout.LayoutParams pLp = lp(-1,dp(76)); pLp.topMargin=dp(12);
        portInput = input("端口", "5005", InputType.TYPE_CLASS_NUMBER);
        card.addView(portInput, pLp);

        LinearLayout switchRow = new LinearLayout(this); switchRow.setGravity(Gravity.CENTER_VERTICAL); switchRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView bgLabel = text("后台运行", 20, Color.rgb(22,26,36), Typeface.NORMAL);
        switchRow.addView(bgLabel, new LinearLayout.LayoutParams(0,-2,1));
        backgroundSwitch = new Switch(this); backgroundSwitch.setChecked(true); switchRow.addView(backgroundSwitch);
        LinearLayout.LayoutParams sLp=lp(-1,-2); sLp.topMargin=dp(14); card.addView(switchRow,sLp);

        destination = text("准备发送", 14, Color.rgb(55,60,72), Typeface.NORMAL);
        LinearLayout.LayoutParams dLp=lp(-1,-2); dLp.topMargin=dp(14); card.addView(destination,dLp);
        sensorStatus = text("加速度计: 检测中 | 陀螺仪: 检测中", 14, Color.rgb(55,60,72), Typeface.NORMAL);
        LinearLayout.LayoutParams stLp=lp(-1,-2); stLp.topMargin=dp(8); card.addView(sensorStatus,stLp);
        packetCount = text("已发送: 0 包", 13, Color.rgb(94,102,120), Typeface.NORMAL);
        LinearLayout.LayoutParams pcLp=lp(-1,-2); pcLp.topMargin=dp(8); card.addView(packetCount,pcLp);
        errorText = text("", 12, Color.rgb(190,45,45), Typeface.NORMAL);
        LinearLayout.LayoutParams eLp=lp(-1,-2); eLp.topMargin=dp(4); card.addView(errorText,eLp);

        LinearLayout dataCard = card();
        LinearLayout.LayoutParams dcLp=lp(-1,-2); dcLp.topMargin=dp(16); root.addView(dataCard,dcLp);
        dataCard.addView(text("实时数据", 20, Color.rgb(22,26,36), Typeface.BOLD));
        values = text("ax = 0.0000\nay = 0.0000\naz = 0.0000\n\ngx = 0.0000\ngy = 0.0000\ngz = 0.0000", 19, Color.rgb(30,35,47), Typeface.MONOSPACE.getStyle());
        values.setTypeface(Typeface.MONOSPACE);
        values.setLineSpacing(dp(3),1.0f);
        LinearLayout.LayoutParams vLp=lp(-1,-2); vLp.topMargin=dp(12); dataCard.addView(values,vLp);

        actionButton = new Button(this);
        actionButton.setText("开始发送"); actionButton.setTextSize(18); actionButton.setTextColor(Color.WHITE); actionButton.setAllCaps(false);
        actionButton.setBackground(round(Color.rgb(10,103,216), 30));
        actionButton.setOnClickListener(v -> toggleSending());
        LinearLayout.LayoutParams bLp=lp(-1,dp(58)); bLp.topMargin=dp(22); root.addView(actionButton,bLp);

        TextView note = text("协议：UDP / JSON / 50 Hz。手机和电脑需连接到同一 Wi‑Fi。", 12, Color.rgb(94,102,120), Typeface.NORMAL);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nLp=lp(-1,-2); nLp.topMargin=dp(12); root.addView(note,nLp);
        return scroll;
    }

    private void toggleSending() {
        if (SensorState.running) {
            Intent i = new Intent(this, SensorSenderService.class); i.setAction(SensorSenderService.ACTION_STOP); startService(i);
            return;
        }
        String host = hostInput.getText().toString().trim();
        if (host.length()==0) { hostInput.setError("请输入电脑 IP"); return; }
        int port;
        try { port = Integer.parseInt(portInput.getText().toString().trim()); } catch (Exception e) { portInput.setError("端口无效"); return; }
        if (port < 1 || port > 65535) { portInput.setError("端口范围 1-65535"); return; }
        Intent i = new Intent(this, SensorSenderService.class);
        i.setAction(SensorSenderService.ACTION_START);
        i.putExtra(SensorSenderService.EXTRA_HOST, host);
        i.putExtra(SensorSenderService.EXTRA_PORT, port);
        if (backgroundSwitch.isChecked() && Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void refresh() {
        destination.setText(SensorState.running ? "Sending to " + SensorState.host + ":" + SensorState.port : "未发送");
        sensorStatus.setText("加速度计: " + (SensorState.accelOk ? "OK" : "不可用") + " | 陀螺仪: " + (SensorState.gyroOk ? "OK" : "不可用"));
        packetCount.setText("已发送: " + SensorState.sentPackets + " 包");
        errorText.setText(SensorState.lastError.length()==0 ? "" : "错误: " + SensorState.lastError);
        values.setText(String.format(Locale.US,
            "ax = %.4f\nay = %.4f\naz = %.4f\n\ngx = %.4f\ngy = %.4f\ngz = %.4f",
            SensorState.ax,SensorState.ay,SensorState.az,SensorState.gx,SensorState.gy,SensorState.gz));
        actionButton.setText(SensorState.running ? "停止" : "开始发送");
        hostInput.setEnabled(!SensorState.running); portInput.setEnabled(!SensorState.running);
    }

    private LinearLayout card() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16),dp(16),dp(16),dp(16)); l.setBackground(round(Color.rgb(233,237,249),20)); return l; }
    private EditText input(String hint, String val, int type) { EditText e=new EditText(this); e.setHint(hint); e.setText(val); e.setTextSize(20); e.setSingleLine(true); e.setInputType(type); e.setPadding(dp(14),0,dp(14),0); GradientDrawable g=round(Color.TRANSPARENT,8); g.setStroke(dp(1),Color.rgb(120,128,147)); e.setBackground(g); return e; }
    private TextView text(String s,int sp,int color,int style) { TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(Typeface.create("sans",style)); return t; }
    private GradientDrawable round(int color,int radius) { GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private LinearLayout.LayoutParams lp(int w,int h) { return new LinearLayout.LayoutParams(w,h); }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    @Override protected void onDestroy() { handler.removeCallbacks(updater); super.onDestroy(); }
}
