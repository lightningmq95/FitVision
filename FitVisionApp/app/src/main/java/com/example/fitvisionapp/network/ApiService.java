package com.example.fitvisionapp.network;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;


import com.example.fitvisionapp.models.ComboImage;

import java.util.HashMap;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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


    @GET("/get-images")
    Call<List<ApiImageTemp>> getImages();

    @GET("/getCombos/{userid}")
    Call<List<String>> getCombos(@Path("userid") String userId);

    @GET("/getComboDetails/{userid}/{comboName}")
    Call<List<ComboImage>> getComboDetails(
            @Path("userid") String userId,
            @Path("comboName") String comboName
    );

    @POST("/addToCombo/{userId}/{comboName}/{clothingId}/{category}")
    Call<Void> addToCombo(
            @Path("userId") String userId,
            @Path("comboName") String comboName,
            @Path("clothingId") String clothingId,
            @Path("category") String category
        );

    @POST("add_to_combo")
    Call<ResponseBody> addToCombo(@Body HashMap<String, String> data);

    @POST("/tryon")
    Call<ResponseBody> tryOnClothing(@Body HashMap<String, String> images);


    @POST("/adjust_lighting")
    Call<ResponseBody> adjustLighting(@Body HashMap<String, String> requestData);

    @POST("/detect-faces")
    Call<List<String>> detectFaces(@Body String base64Image);

    @POST("/tryon-face")
    Call<ResponseBody> tryOnSelectedFace(@Body String base64SelectedFace);

    @POST("/cvtransform")
    Call<ResponseBody> cvTransform(@Body Map<String, String> body);



}