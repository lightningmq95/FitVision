package com.example.fitvisionapp.ui.tryon;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
    private String userImageName = null, clothingImageName = null;

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
            if (userImageName != null && clothingImageName != null) {
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

                String imageName = getFileNameFromUri(imageUri);

                if (requestCode == PICK_USER_IMAGE) {
                    imageUser.setImageBitmap(bitmap);
                    userImageName = imageName;
                } else if (requestCode == PICK_CLOTHING_IMAGE) {
                    imageClothing.setImageBitmap(bitmap);
                    clothingImageName = imageName;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error selecting image", e);
            }
        }
    }

    // Get file name from Uri
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    // Send image names to backend
    private void sendTryOnRequest() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        HashMap<String, String> requestData = new HashMap<>();

        requestData.put("clothing_image_name", clothingImageName);
        requestData.put("user_image_name", userImageName);

        Call<ResponseBody> call = apiService.tryOnClothing(requestData);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        String base64Image = jsonResponse.getString("output_image");

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
