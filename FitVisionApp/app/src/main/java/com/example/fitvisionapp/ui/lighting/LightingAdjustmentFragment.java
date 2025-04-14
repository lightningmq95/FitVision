package com.example.fitvisionapp.ui.lighting;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;
import com.example.fitvisionapp.network.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LightingAdjustmentFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imagePreview, resultImage;
    private EditText lightingTypeInput;
    private Button selectImageButton, adjustLightingButton;
    private Bitmap selectedBitmap;

    public LightingAdjustmentFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lighting_adjustment, container, false);

        imagePreview = view.findViewById(R.id.image_preview);
        resultImage = view.findViewById(R.id.result_image);
        lightingTypeInput = view.findViewById(R.id.lighting_type_input);
        selectImageButton = view.findViewById(R.id.select_image_button);
        adjustLightingButton = view.findViewById(R.id.adjust_lighting_button);

        selectImageButton.setOnClickListener(v -> selectImage());
        adjustLightingButton.setOnClickListener(v -> sendLightingAdjustmentRequest());

        return view;
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                imagePreview.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendLightingAdjustmentRequest() {
        if (selectedBitmap == null) {
            Toast.makeText(getContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String lightingType = lightingTypeInput.getText().toString().trim();
        if (lightingType.isEmpty()) {
            Toast.makeText(getContext(), "Enter a lighting type", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = encodeImageToBase64(selectedBitmap);

        HashMap<String, String> requestData = new HashMap<>();
        requestData.put("image", imageBase64);
        requestData.put("lighting_type", lightingType);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<ResponseBody> call = apiService.adjustLighting(requestData);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] imageBytes = response.body().bytes();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        resultImage.setImageBitmap(bitmap);
                        Toast.makeText(getContext(), "Lighting adjusted!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("LightingAdjustment", "Error processing image", e);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to adjust lighting", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}
