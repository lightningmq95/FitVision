package com.example.fitvisionapp.network;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    // Method 1: Send multiple images and username, get category & color
    @Multipart
    @POST("/analyze")
    Call<List<Map<String, String>>> analyzeImages(
            @Part("username") RequestBody username,
            @Part List<MultipartBody.Part> images
    );

    // Method 2: Send corrected category, color, and image name
    @POST("/sendCorrections")
    Call<Map<String, String>> sendCorrections(@Body List<Map<String, String>> corrections);
}
