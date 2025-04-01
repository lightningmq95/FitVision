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
    @POST("/upload-image/{userid}")
    Call<Map<String, String>> uploadImage(
            @Path("userid") String userId,
            @Part MultipartBody.Part image,
            @Part("image_name") RequestBody imageName  // Check if backend expects String instead
    );

    @POST("/sendCorrection")
    Call<Map<String, String>> sendCorrection(@Body Map<String, String> data);


    // Add this method to retrieve images
    @GET("/get-images")  // Ensure this matches your backend endpoint
    Call<List<ApiImageTemp>> getImages();

    @GET("/getCombos/{userid}")
    Call<List<String>> getCombos(@Path("userid") String userId);

    @GET("/getComboDetails/{userid}/{comboName}")
    Call<List<ComboImage>> getComboDetails(
            @Path("userid") String userId,
            @Path("comboName") String comboName
    );

}