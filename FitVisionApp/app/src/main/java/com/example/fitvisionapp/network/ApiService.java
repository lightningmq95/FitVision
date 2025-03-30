package com.example.fitvisionapp.network;

import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Body;

public interface ApiService {

    @Multipart
    @POST("/analyze")
    Call<Map<String, String>> analyzeImage(
            @Part MultipartBody.Part image
    );

    @POST("/sendCorrection")
    Call<Map<String, String>> sendCorrection(@Body Map<String, String> data);
}
