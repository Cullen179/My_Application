package com.example.myapplication.ui.dashboard;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.api.DeepfakeApiClient;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UploadMediaFragment extends Fragment {

    private static final String TAG = "UploadMediaFragment";
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_IMAGE_PICK = 200;
    private static final int REQUEST_CAMERA_PERMISSION = 300;
    private static final int REQUEST_IMAGE_CAPTURE = 400;
    private static final int REQUEST_MEDIA_IMAGES_PERMISSION = 500;

    // UI components
    private ImageView imagePreview;
    private LinearProgressIndicator fakeProgressIndicator;
    private LinearProgressIndicator realProgressIndicator;
    private TextView fakePercentageText;
    private TextView realPercentageText;
    private ImageView explanationImageView;
    private FrameLayout loadingOverlay;
    private RecyclerView recentUploadsRecyclerView;
    private TextView classificationResultText;

    // API client for deepfake detection
    private DeepfakeApiClient apiClient;
    
    // Recent uploads adapter
    private RecentUploadsAdapter recentUploadsAdapter;
    private List<UploadItem> recentUploads = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_upload_media, container, false);
        
        // Initialize API client
        apiClient = new DeepfakeApiClient(requireContext());
        
        // Initialize UI components
        imagePreview = root.findViewById(R.id.imagePreview);
        fakeProgressIndicator = root.findViewById(R.id.fakeProgressIndicator);
        realProgressIndicator = root.findViewById(R.id.realProgressIndicator);
        fakePercentageText = root.findViewById(R.id.fakePercentageText);
        realPercentageText = root.findViewById(R.id.realPercentageText);
        explanationImageView = root.findViewById(R.id.explanationImageView);
        loadingOverlay = root.findViewById(R.id.loadingOverlay);
        recentUploadsRecyclerView = root.findViewById(R.id.recentUploadsRecyclerView);
        classificationResultText = root.findViewById(R.id.classificationResultText);
        
        // Initialize gallery button
        root.findViewById(R.id.galleryButton).setOnClickListener(v -> checkStoragePermissionAndPickImage());
        
        // Initialize camera button
        root.findViewById(R.id.cameraButton).setOnClickListener(v -> checkCameraPermissionAndTakePhoto());
        
        // Initialize recent uploads recycler view
        recentUploadsAdapter = new RecentUploadsAdapter(recentUploads);
        recentUploadsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentUploadsRecyclerView.setAdapter(recentUploadsAdapter);
        
        return root;
    }
    
    private void checkStoragePermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), 
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 
                        REQUEST_MEDIA_IMAGES_PERMISSION);
            } else {
                openGalleryPicker();
            }
        } else {
            // Older Android versions use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), 
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                        REQUEST_STORAGE_PERMISSION);
            } else {
                openGalleryPicker();
            }
        }
    }
    
    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.CAMERA}, 
                    REQUEST_CAMERA_PERMISSION);
        } else {
            openCameraCapture();
        }
    }
    
    private void openGalleryPicker() {
        // Create intent for picking images from gallery
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Set MIME type for images
        intent.setType("image/*");
        // Multiple file selection is disabled
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_IMAGE_PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No file manager app installed", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openCameraCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(requireContext(), "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION || requestCode == REQUEST_MEDIA_IMAGES_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGalleryPicker();
            } else {
                Toast.makeText(requireContext(), "Storage permission required to select images", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraCapture();
            } else {
                Toast.makeText(requireContext(), "Camera permission required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == getActivity().RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "Failed to get image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Grant read permission to this URI for our app
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            requireActivity().getContentResolver().takePersistableUriPermission(
                selectedImageUri, takeFlags);
            
            // Display selected image
            try {
                InputStream imageStream = requireActivity().getContentResolver().openInputStream(selectedImageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imagePreview.setImageBitmap(selectedImage);
                
                // Upload image to backend for deepfake detection
                uploadImageForDeepfakeDetection(selectedImageUri);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
                Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK && data != null) {
            // Get image from camera
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imagePreview.setImageBitmap(imageBitmap);
                
                // Convert bitmap to URI for upload
                Uri imageUri = getImageUriFromBitmap(imageBitmap);
                if (imageUri != null) {
                    uploadImageForDeepfakeDetection(imageUri);
                } else {
                    Toast.makeText(requireContext(), "Error processing camera image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(
                    requireActivity().getContentResolver(), 
                    bitmap, 
                    "DeepfakeDetection_" + System.currentTimeMillis(), 
                    null);
            return Uri.parse(path);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to URI: " + e.getMessage());
            return null;
        }
    }
    
    private void uploadImageForDeepfakeDetection(Uri imageUri) {
        // Show loading state
        loadingOverlay.setVisibility(View.VISIBLE);
        resetResults();
        
        // Use the API client to upload and detect deepfake
        apiClient.detectDeepfake(imageUri, new DeepfakeApiClient.DeepfakeDetectionCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                loadingOverlay.setVisibility(View.GONE);
                
                try {
                    // Extract response data
                    JSONObject confidences = response.getJSONObject("confidences");
                    String predictedClass = response.getString("predicted_class");
                    String faceWithMaskBase64 = response.optString("face_with_mask_base64", null);
                    
                    // Update UI with results
                    updateResultsUI(confidences, predictedClass, faceWithMaskBase64);
                    
                    // Add to recent uploads
                    addToRecentUploads(imageUri, predictedClass.equals("real") ? "True" : "False");
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void resetResults() {
        fakeProgressIndicator.setProgress(0);
        realProgressIndicator.setProgress(0);
        fakePercentageText.setText("0%");
        realPercentageText.setText("0%");
        classificationResultText.setText("");
        explanationImageView.setImageDrawable(null);
    }
    
    private void updateResultsUI(JSONObject confidences, String predictedClass, String faceWithMaskBase64) {
        try {
            // Update progress indicators
            int fakePercentage = (int) (confidences.optDouble("fake", 0) * 100);
            int realPercentage = (int) (confidences.optDouble("real", 0) * 100);
            
            fakeProgressIndicator.setProgress(fakePercentage);
            realProgressIndicator.setProgress(realPercentage);
            fakePercentageText.setText(fakePercentage + "%");
            realPercentageText.setText(realPercentage + "%");
            
            // Update classification result
            classificationResultText.setText(predictedClass.substring(0, 1).toUpperCase() + predictedClass.substring(1));
            
            // Display explanation heatmap if available
            if (faceWithMaskBase64 != null && !faceWithMaskBase64.isEmpty()) {
                byte[] decodedBytes = Base64.decode(faceWithMaskBase64, Base64.DEFAULT);
                Bitmap heatmapBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                explanationImageView.setImageBitmap(heatmapBitmap);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating results UI: " + e.getMessage());
        }
    }
    
    private void addToRecentUploads(Uri imageUri, String classification) {
        try {
            // Create thumbnail from original image
            InputStream imageStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);
            
            // Add to recent uploads list
            String currentDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());
            UploadItem newItem = new UploadItem("Random.jpg", classification, currentDate, originalBitmap);
            
            // Add to beginning of list
            recentUploads.add(0, newItem);
            recentUploadsAdapter.notifyItemInserted(0);
            
            // Limit list size
            if (recentUploads.size() > 10) {
                recentUploads.remove(recentUploads.size() - 1);
                recentUploadsAdapter.notifyItemRemoved(recentUploads.size());
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error creating thumbnail: " + e.getMessage());
        }
    }
    
    // Model class for recent uploads list items
    public static class UploadItem {
        private String filename;
        private String classification;
        private String uploadDate;
        private Bitmap thumbnail;
        
        public UploadItem(String filename, String classification, String uploadDate, Bitmap thumbnail) {
            this.filename = filename;
            this.classification = classification;
            this.uploadDate = uploadDate;
            this.thumbnail = thumbnail;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public String getClassification() {
            return classification;
        }
        
        public String getUploadDate() {
            return uploadDate;
        }
        
        public Bitmap getThumbnail() {
            return thumbnail;
        }
    }
    
    // Adapter for the recent uploads RecyclerView
    public class RecentUploadsAdapter extends RecyclerView.Adapter<RecentUploadsAdapter.ViewHolder> {
        private List<UploadItem> uploadItems;
        
        public RecentUploadsAdapter(List<UploadItem> uploadItems) {
            this.uploadItems = uploadItems;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_upload, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UploadItem item = uploadItems.get(position);
            holder.thumbnailImageView.setImageBitmap(item.getThumbnail());
            holder.filenameTextView.setText(item.getFilename());
            holder.classificationTextView.setText("Classification: " + item.getClassification());
            holder.uploadDateTextView.setText("Date Upload: " + item.getUploadDate());
            
            holder.deleteButton.setOnClickListener(v -> {
                uploadItems.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, uploadItems.size());
            });
        }
        
        @Override
        public int getItemCount() {
            return uploadItems.size();
        }
        
        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnailImageView;
            TextView filenameTextView;
            TextView classificationTextView;
            TextView uploadDateTextView;
            ImageView deleteButton;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                thumbnailImageView = itemView.findViewById(R.id.thumbnailImageView);
                filenameTextView = itemView.findViewById(R.id.filenameTextView);
                classificationTextView = itemView.findViewById(R.id.classificationTextView);
                uploadDateTextView = itemView.findViewById(R.id.uploadDateTextView);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
} 