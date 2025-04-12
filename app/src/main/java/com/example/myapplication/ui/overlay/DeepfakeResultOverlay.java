package com.example.myapplication.ui.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.myapplication.R;

public class DeepfakeResultOverlay {
    private final Context context;
    private final WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;
    private TextView tvResultType, tvFakePercentage, tvRealPercentage;
    private ProgressBar progressBar;
    private boolean isShowing = false;

    public DeepfakeResultOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initOverlay();
    }

    private void initOverlay() {
        // Inflate the overlay layout
        overlayView = LayoutInflater.from(context).inflate(R.layout.deepfake_result_overlay, null);
        
        // Get references to views
        tvResultType = overlayView.findViewById(R.id.tvResultType);
        tvFakePercentage = overlayView.findViewById(R.id.tvFakePercentage);
        tvRealPercentage = overlayView.findViewById(R.id.tvRealPercentage);
        progressBar = overlayView.findViewById(R.id.progressBar);

        // Configure window parameters
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP;
        params.y = 100; // Position from top
    }

    public void show() {
        if (!isShowing) {
            windowManager.addView(overlayView, params);
            isShowing = true;
        }
    }

    public void hide() {
        if (isShowing && overlayView != null) {
            windowManager.removeView(overlayView);
            isShowing = false;
        }
    }

    public void updateResults(float fakePercentage, float realPercentage) {
        if (tvFakePercentage == null || tvRealPercentage == null || progressBar == null) return;
        
        // Update text values
        tvFakePercentage.setText(String.format("%.0f%%", fakePercentage));
        tvRealPercentage.setText(String.format("%.0f%%", realPercentage));
        
        // Update progress bar
        progressBar.setProgress((int) fakePercentage);
        
        // Set the main result type based on which percentage is higher
        if (fakePercentage >= realPercentage) {
            tvResultType.setText("Fake");
            tvResultType.setTextColor(context.getResources().getColor(android.R.color.holo_purple));
        } else {
            tvResultType.setText("Real");
            tvResultType.setTextColor(context.getResources().getColor(android.R.color.white));
        }
        
        // Show the overlay
        if (!isShowing) {
            show();
        }
    }
} 