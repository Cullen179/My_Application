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
    private boolean mImageReaderValid = false;  // Flag to track ImageReader validity
    private DeepfakeApiClient apiClient;  // API client
    private DeepfakeResultOverlay mDeepfakeOverlay; // Deepfake result overlay

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
        mDeepfakeOverlay = new DeepfakeResultOverlay(this); // Initialize the overlay

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
        try {
            // Save bitmap to a temporary file
            File outputDir = getExternalCacheDir();
            File outputFile = File.createTempFile("tmp_", ".jpg", outputDir);
            
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            // Use our API client to upload the image
            apiClient.detectDeepfake(Uri.fromFile(outputFile), new DeepfakeApiClient.DeepfakeDetectionCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        Log.d(TAG, "Deepfake detection success: " + response.toString());
                        
                        // Extract confidence scores
                        JSONObject confidences = response.getJSONObject("confidences");
                        float fakePercentage = 0f;
                        float realPercentage = 0f;
                        
                        if (confidences.has("fake")) {
                            fakePercentage = (float) (confidences.getDouble("fake") * 100);
                        }
                        
                        if (confidences.has("real")) {
                            realPercentage = (float) (confidences.getDouble("real") * 100);
                        }
                        
                        // Update the deepfake result overlay
                        if (mDeepfakeOverlay != null) {
                            final float finalFakePercentage = fakePercentage;
                            final float finalRealPercentage = realPercentage;
                            mHandler.post(() -> mDeepfakeOverlay.updateResults(finalFakePercentage, finalRealPercentage));
                            
                            // Hide the overlay after the call ends or after a timeout
                            mHandler.postDelayed(() -> {
                                if (mDeepfakeOverlay != null) {
                                    mDeepfakeOverlay.hide();
                                }
                            }, 30000); // Hide after 30 seconds
                        }
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing deepfake response: " + e.getMessage());
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Deepfake detection error: " + error);
                }
            });
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap for deepfake detection: " + e.getMessage());
        }
    }

    private void stopCapturing() {
        if (!mIsCapturing) {
            return;
        }

        Log.d(TAG, "Stopping screenshot capture");
        mIsCapturing = false;
        mHandler.removeCallbacks(mScreenshotRunnable);

        // Clean up ImageReader
        if (mImageReader != null) {
            mImageReaderValid = false;  // Set flag before closing
            mImageReader.close();
            mImageReader = null;
        }

        // Clean up VirtualDisplay
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        // Clean up MediaProjection
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        
        // Hide deepfake overlay
        if (mDeepfakeOverlay != null) {
            mDeepfakeOverlay.hide();
        }

        // Stop foreground service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "Screenshot capture stopped");
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
        super.onDestroy();
        if (mIsCapturing) {
            stopCapturing();
        }
        
        // Ensure overlay is hidden when service is destroyed
        if (mDeepfakeOverlay != null) {
            mDeepfakeOverlay.hide();
            mDeepfakeOverlay = null;
        }
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