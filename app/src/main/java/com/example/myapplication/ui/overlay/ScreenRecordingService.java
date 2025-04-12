package com.example.myapplication.ui.overlay;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.api.DeepfakeApiClient;
import com.example.myapplication.data.model.DeepfakeResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.nio.ByteBuffer;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ScreenRecordingService extends Service {
    public static final int SCREEN_RECORD_REQUEST_CODE = 1001;
    private static final String TAG = "ScreenRecordingService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "screen_recording_channel";
    private static final int SCREENSHOT_INTERVAL = 3000; // 3 seconds

    // Add static fields to store permission data
    static int sResultCode = Activity.RESULT_CANCELED;
    static Intent sData = null;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private int mScreenDensity;
    private int mScreenWidth;
    private int mScreenHeight;
    private String mOutputPath;
    private boolean mIsCapturing = false;
    private Handler mHandler;
    private Runnable mScreenshotRunnable;
    private ImageReader mImageReader;
    private boolean mImageReaderValid = false;  // Add flag to track ImageReader validity
    private DeepfakeApiClient apiClient;  // Add API client

    // Add MediaProjection callback
    private final MediaProjection.Callback mMediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            if (mIsCapturing) {
                stopCapturing();
            }
            mMediaProjection = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        apiClient = new DeepfakeApiClient(this);  // Pass the Service context

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        // Create output directory if it doesn't exist
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CallScreenshots");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory");
            }
        }

        // Create output file name
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        mOutputPath = mediaStorageDir.getPath() + File.separator + "SCR_" + timestamp + ".jpg";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        System.out.println("ScreenRecordingService received action: " + action);
        
        if ("START_RECORDING".equals(action)) {
            System.out.println("Processing START_RECORDING command");
            // Get media projection data from the intent
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            System.out.println("Received resultCode: " + resultCode + ", data: " + (data != null));
            
            // Store the permission data if it's valid
            if (resultCode == Activity.RESULT_OK && data != null) {
                System.out.println("Storing valid permission data");
                sResultCode = resultCode;
                sData = data;
            }
            
            // Use stored permission data if available
            if (sResultCode == Activity.RESULT_OK && sData != null) {
                System.out.println("Starting capturing with stored permission data");
                startCapturing(sResultCode, sData);
            } else {
                System.out.println("Cannot start capturing - missing valid permission data");
            }
        } else if ("STOP_RECORDING".equals(action)) {
            System.out.println("Processing STOP_RECORDING command");
            stopCapturing();
        }
        return START_STICKY;
    }

    private void startCapturing(int resultCode, Intent data) {
        if (mIsCapturing) {
            return;
        }

        Log.d(TAG, "Starting screenshot capture");
        // Create foreground notification
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Create media projection
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        if (mMediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            return;
        }

        // Register the callback
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);

        // Create ImageReader
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, 
                PixelFormat.RGBA_8888, 2);
        mImageReaderValid = true;  // Set flag when ImageReader is created

        // Create virtual display
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenCapture",
                mScreenWidth,
                mScreenHeight,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(),
                null,
                null
        );

        mIsCapturing = true;

        // Start periodic screenshot capture
        mScreenshotRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsCapturing && mImageReaderValid) {  // Check both flags
                    takeScreenshot();
                    mHandler.postDelayed(this, SCREENSHOT_INTERVAL);
                }
            }
        };
        mHandler.post(mScreenshotRunnable);

        Log.d(TAG, "Screenshot capture started");
    }

    private void takeScreenshot() {
        if (!mIsCapturing || !mImageReaderValid || mImageReader == null) {  // Check all conditions
            return;
        }

        try {
            // Acquire the latest image
            Image image = mImageReader.acquireLatestImage();
            if (image == null) {
                return;
            }

            try {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mScreenWidth;

                    // Create a bitmap
                    Bitmap bitmap = Bitmap.createBitmap(
                            mScreenWidth + rowPadding / pixelStride,
                            mScreenHeight,
                            Bitmap.Config.ARGB_8888
                    );
                    bitmap.copyPixelsFromBuffer(buffer);

                    // Crop to actual screen size
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, mScreenWidth, mScreenHeight);
                    bitmap.recycle();

                    // Save the bitmap to a file
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                    String outputPath = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES).getPath() + 
                            File.separator + "CallScreenshots" + 
                            File.separator + "SCR_" + timestamp + ".jpg";
                    
                    File outputDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "CallScreenshots");
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                    }
                    
                    FileOutputStream fos = new FileOutputStream(outputPath);
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();

                    // Create a copy of the bitmap for deepfake detection
                    Bitmap deepfakeBitmap = croppedBitmap.copy(croppedBitmap.getConfig(), true);
                    croppedBitmap.recycle();

                    // Upload image for deepfake detection
                    uploadImageForDeepfakeDetection(deepfakeBitmap);

                    // Notify that the screenshot was saved
                    MediaScannerConnection.scanFile(
                            this,
                            new String[]{outputPath},
                            null,
                            (path, uri) -> {
                                Log.i(TAG, "Screenshot saved: " + path);
                            }
                    );
                }
            } finally {
                // Always close the image
                image.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot", e);
            // If we get an error, invalidate the ImageReader
            mImageReaderValid = false;
        }
    }

    private void uploadImageForDeepfakeDetection(Bitmap bitmap) {
        new Thread(() -> {
            try {
                // Save bitmap to a temporary file
                File tempFile = new File(getCacheDir(), "temp_screenshot.jpg");
                FileOutputStream fos = new FileOutputStream(tempFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                // Create Uri from the temporary file
                Uri imageUri = Uri.fromFile(tempFile);

                // Create callback for deepfake detection
                DeepfakeApiClient.DeepfakeDetectionCallback callback = new DeepfakeApiClient.DeepfakeDetectionCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Intent intent = new Intent(ScreenRecordingService.this, FloatingService.class);
                        JSONObject confidences = null;
                        String predictedClass = null;
                        try {
                            // Extract response data
                            confidences = response.getJSONObject("confidences");
                            predictedClass = response.getString("predicted_class");
                            String faceWithMaskBase64 = response.optString("face_with_mask_base64", null);

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                        }
                        intent.putExtra("fakePercentage", (int) (confidences.optDouble("fake", 0) * 100));
                        intent.putExtra("realPercentage", (int) (confidences.optDouble("real", 0) * 100));
                        intent.putExtra("predictedClass", predictedClass.substring(0, 1).toUpperCase() + predictedClass.substring(1));
                        startService(intent);

                        // Clean up
                        tempFile.delete();
                        bitmap.recycle();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error uploading image for deepfake detection: " + error);
                        // Clean up
                        tempFile.delete();
                        bitmap.recycle();
                    }
                };

                // Make API call using DeepfakeApiClient
                apiClient.detectDeepfake(imageUri, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error uploading image for deepfake detection", e);
                // Clean up in case of error
                bitmap.recycle();
            }
        }).start();
    }

    private void stopCapturing() {
        if (mIsCapturing) {
            mIsCapturing = false;
            mImageReaderValid = false;  // Invalidate ImageReader first
            mHandler.removeCallbacks(mScreenshotRunnable);
            
            // Clean up ImageReader
            if (mImageReader != null) {
                try {
                    mImageReader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing ImageReader", e);
                }
                mImageReader = null;
            }
            
            // Clean up virtual display
            if (mVirtualDisplay != null) {
                try {
                    mVirtualDisplay.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing VirtualDisplay", e);
                }
                mVirtualDisplay = null;
            }
            
            // Clean up media projection
            if (mMediaProjection != null) {
                try {
                    mMediaProjection.stop();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping MediaProjection", e);
                }
                mMediaProjection = null;
            }
            
            stopForeground(true);
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Capture Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Screen capture service notification");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture")
                .setContentText("Taking screenshots...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Add a stop capture action
        Intent stopIntent = new Intent(this, ScreenRecordingService.class);
        stopIntent.setAction("STOP_RECORDING");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(android.R.drawable.ic_media_pause, "Stop Capture", stopPendingIntent);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopCapturing();
        super.onDestroy();
    }

    // Add method to request screen recording permission
    public static void requestScreenRecordingPermission(Activity activity) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) 
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            SCREEN_RECORD_REQUEST_CODE
        );
    }

    // Add method to handle permission result
    public static void handlePermissionResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            sResultCode = resultCode;
            sData = data;
            System.out.println("Screen recording permission granted and stored");
        } else {
            System.out.println("Screen recording permission denied");
        }
    }
}