package com.example.myapplication.data.api;

import com.example.myapplication.data.model.DeepfakeResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("detect")
    Call<DeepfakeResponse> uploadImage(@Part MultipartBody.Part image);
}