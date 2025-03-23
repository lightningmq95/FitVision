package com.example.fitvisionapp.network;

import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Body;

public interface ApiService {

    // ✅ Fix: Removed username! Now it only expects multiple images
    @Multipart
    @POST("/analyze")
    Call<List<Map<String, String>>> analyzeImages(
            @Part List<MultipartBody.Part> images
    );

    // ✅ Fix: Ensure corrections are sent correctly
    @POST("/sendCorrections")
    Call<Map<String, String>> sendCorrections(@Body List<Map<String, String>> corrections);
}
