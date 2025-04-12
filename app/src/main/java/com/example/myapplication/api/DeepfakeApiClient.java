package com.example.myapplication.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeepfakeApiClient {
    private static final String TAG = "DeepfakeApiClient";
    private static final String BASE_URL = "http://127.0.0.1:8000"; // For emulator to access localhost
    // private static final String BASE_URL = "http://10.0.2.2:8000"; // For emulator to access localhost
    private static final int DEFAULT_TIMEOUT = 60000; // 60 seconds for image upload

    private final RequestQueue requestQueue;
    private final Context context;

    public DeepfakeApiClient(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public void detectDeepfake(Uri imageUri, final DeepfakeDetectionCallback callback) {
        try {
            // Prepare image data for upload
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            final byte[] imageData = byteArrayOutputStream.toByteArray();
            inputStream.close();

            // Create a custom volley request to upload the image
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                    Request.Method.POST,
                    BASE_URL + "/deepfake/detect",
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            // Parse response
                            String responseStr = new String(response.data);
                            try {
                                JSONObject jsonResponse = new JSONObject(responseStr);
                                callback.onSuccess(jsonResponse);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
                                callback.onError("Error parsing response: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error uploading image: " + error.getMessage());
                            callback.onError("Error uploading image: " + 
                                    (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                        }
                    }
            ) {
                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    String fileName = "image_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
                    params.put("file", new DataPart(fileName, imageData, "image/jpeg"));
                    return params;
                }
            };

            // Add retry policy with longer timeout for image upload
            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    DEFAULT_TIMEOUT,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Add the request to the queue
            requestQueue.add(multipartRequest);

        } catch (IOException e) {
            Log.e(TAG, "Error reading image data: " + e.getMessage());
            callback.onError("Error reading image data: " + e.getMessage());
        }
    }

    // Callback interface for deepfake detection API
    public interface DeepfakeDetectionCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    // Custom class for uploading multipart data with Volley
    public static class VolleyMultipartRequest extends Request<NetworkResponse> {
        private final Response.Listener<NetworkResponse> mListener;
        private final Map<String, String> mStringParams = new HashMap<>();
        private final Map<String, DataPart> mDataParams = new HashMap<>();

        public VolleyMultipartRequest(int method, String url, Response.Listener<NetworkResponse> listener,
                                     Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            mListener = listener;
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            return headers;
        }

        protected Map<String, DataPart> getByteData() {
            return mDataParams;
        }

        @Override
        protected void deliverResponse(NetworkResponse response) {
            mListener.onResponse(response);
        }

        @Override
        protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
            return Response.success(response, null);
        }

        @Override
        public String getBodyContentType() {
            return "multipart/form-data; boundary=" + "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        }

        @Override
        public byte[] getBody() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                // Write string params
                for (Map.Entry<String, String> entry : mStringParams.entrySet()) {
                    bos.write(("--" + boundary + "\r\n").getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes());
                    bos.write((entry.getValue() + "\r\n").getBytes());
                }

                // Write data params
                for (Map.Entry<String, DataPart> entry : getByteData().entrySet()) {
                    DataPart dataFile = entry.getValue();
                    bos.write(("--" + boundary + "\r\n").getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey()
                            + "\"; filename=\"" + dataFile.getFileName() + "\"\r\n").getBytes());
                    bos.write(("Content-Type: " + dataFile.getMimeType() + "\r\n\r\n").getBytes());
                    bos.write(dataFile.getContent());
                    bos.write(("\r\n").getBytes());
                }

                // End boundary
                bos.write(("--" + boundary + "--\r\n").getBytes());

            } catch (IOException e) {
                e.printStackTrace();
            }
            return bos.toByteArray();
        }

        public static class DataPart {
            private String fileName;
            private byte[] content;
            private String mimeType;

            public DataPart(String name, byte[] data, String mimeType) {
                fileName = name;
                content = data;
                this.mimeType = mimeType;
            }

            public String getFileName() {
                return fileName;
            }

            public byte[] getContent() {
                return content;
            }

            public String getMimeType() {
                return mimeType;
            }
        }
    }
} 