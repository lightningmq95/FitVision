package com.example.fitvisionapp.network;

import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @Multipart
    @POST("/upload-image/{userid}")
    Call<Map<String, String>> uploadImage(
            @Path("userid") String userId,
            @Part MultipartBody.Part image,
            @Part("image_name") RequestBody imageName  // NEW: Send image name
    );

    @POST("/sendCorrection")
    Call<Map<String, String>> sendCorrection(@retrofit2.http.Body Map<String, String> data);
}
