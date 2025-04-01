package com.example.fitvisionapp.network;

import com.example.fitvisionapp.models.ComboImage;

import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import java.util.List;
import retrofit2.http.GET;

public interface ApiService {

    @Multipart
    @POST("/upload-image/{userId}")
    Call<Map<String, String>> uploadImage(
            @Path("userId") String userId,
            @Part MultipartBody.Part image,
            @Part("image_name") RequestBody imageName  // Check if backend expects String instead
    );

    @POST("/sendCorrection")
    Call<Map<String, String>> sendCorrection(@Body Map<String, String> data);


    // Add this method to retrieve images
    @GET("/images/devastatingrpg")  // Ensure this matches your backend endpoint
    Call<List<ApiImageTemp>> getImages();

    @GET("/combos/devastatingrpg")
    Call<List<String>> getCombos();

    @GET("/combos/devastatingrpg/{comboName}")
    Call<Map<String, Object>> getComboDetails(
//            @Path("userid") String userId,
            @Path("comboName") String comboName
    );

}