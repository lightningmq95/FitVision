package com.example.fitvisionapp.ui.tryon;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;
import com.example.fitvisionapp.network.RetrofitClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TryOnFragment extends Fragment {

    private static final int PICK_USER_IMAGE = 1;
    private static final int PICK_CLOTHING_IMAGE = 2;
    private static final String TAG = "TryOnFragment";

    private ImageView imageUser, imageClothing, imageResult;
    private Button btnTryOn, btnBack;
    private String userBase64 = null, clothingBase64 = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tryon, container, false);

        imageUser = view.findViewById(R.id.imageUser);
        imageClothing = view.findViewById(R.id.imageClothing);
        imageResult = view.findViewById(R.id.imageResult);
        btnTryOn = view.findViewById(R.id.btnTryOn);
        btnBack = view.findViewById(R.id.btnBack);

        // Select user image
        imageUser.setOnClickListener(v -> pickImage(PICK_USER_IMAGE));

        // Select clothing image
        imageClothing.setOnClickListener(v -> pickImage(PICK_CLOTHING_IMAGE));

        // Send images for try-on
        btnTryOn.setOnClickListener(v -> {
            if (userBase64 != null && clothingBase64 != null) {
                sendTryOnRequest();
            } else {
                Toast.makeText(getContext(), "Select both images first!", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    // Pick Image from Gallery
    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    // Handle Image Selection
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                String base64Image = encodeToBase64(bitmap);

                if (requestCode == PICK_USER_IMAGE) {
                    imageUser.setImageBitmap(bitmap);
                    userBase64 = base64Image;
                } else if (requestCode == PICK_CLOTHING_IMAGE) {
                    imageClothing.setImageBitmap(bitmap);
                    clothingBase64 = base64Image;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error selecting image", e);
            }
        }
    }

    // Convert Bitmap to Base64
    private String encodeToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Send images to backend
    private void sendTryOnRequest() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        HashMap<String, String> requestData = new HashMap<>();
        requestData.put("user_image", userBase64);
        requestData.put("clothing_image", clothingBase64);

        Call<ResponseBody> call = apiService.tryOnClothing(requestData);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Get response as a JSON string
                        String responseString = response.body().string();
                        Log.d(TAG, "Server Response: " + responseString.substring(0, Math.min(100, responseString.length())));

                        // Extract "output_image" from JSON
                        JSONObject jsonResponse = new JSONObject(responseString);
                        String base64Image = jsonResponse.getString("output_image");

                        // Convert Base64 to Bitmap
                        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                        imageResult.setImageBitmap(bitmap);
                        Toast.makeText(getContext(), "Try-On Success!", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image", e);
                        Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Try-On Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }



            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



}
