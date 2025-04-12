package com.example.myapplication.ui.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;

public class FloatingService extends android.app.Service {

    private WindowManager windowManager;
    private View floatingView, callMessageView;
    private WindowManager.LayoutParams iconParams, messageParams;
    View floatingIcon;
    private TextView callMessage;
    int lastX, lastY;

    @Override
    public void onCreate() {
        super.onCreate();
        createOverlay();
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("CALL_STATE")) {
            String message = intent.getStringExtra("CALL_STATE");
            handleCallStateChange(message);
        }
        return START_STICKY;
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // Inflate floating icon layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_icon, null);
        floatingIcon = floatingView.findViewById(R.id.floating_icon);

        // Inflate call message layout
        callMessageView = LayoutInflater.from(this).inflate(R.layout.call_message, null);
        callMessage = callMessageView.findViewById(R.id.call_message);
        callMessage.setVisibility(View.GONE); // Initially hidden

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Floating icon layout params
        iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 100;
        iconParams.y = 300; // Default position

        // Message layout params (will appear just above the icon)
        messageParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        messageParams.gravity = Gravity.TOP | Gravity.START;

        // Add views to WindowManager
        windowManager.addView(floatingView, iconParams);  // Add floating icon first
        windowManager.addView(callMessageView, messageParams);  // Add message after

        // 🟢 **Now call `updateMessagePosition()` after adding the views**
        updateMessagePosition();

        // Handle dragging the floating icon
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = iconParams.x;
                        initialY = iconParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        iconParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        iconParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, iconParams);
                        updateMessagePosition(); // 🟢 Move message with the icon
                        return true;
                }
                return false;
            }
        });
    }

    private void updateMessagePosition() {
        messageParams.x = iconParams.x;
        messageParams.y = iconParams.y - 100; // Position just above the icon
        windowManager.updateViewLayout(callMessageView, messageParams);
    }

    private void handleCallStateChange(String message) {
        if ("Call in Progress...".equals(message)) {
            showCallMessage(message);
        } else {
            hideCallMessage();
        }
    }

    private void showCallMessage(String message) {
        if (callMessage != null) {
            callMessage.setText(message);
            callMessage.setVisibility(View.VISIBLE);
        }
    }

    private void hideCallMessage() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (callMessage != null) {
                callMessage.setVisibility(View.GONE);
            }
        }, 3000);
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "floating_service_channel";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Floating Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Floating Service")
                    .setContentText("Overlay is running")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
}
