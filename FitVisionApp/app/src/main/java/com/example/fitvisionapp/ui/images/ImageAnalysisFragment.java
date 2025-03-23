package com.example.fitvisionapp.ui.images;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageAnalysisFragment extends Fragment {

    private LinearLayout imagePreviewLayout, backendImageLayout;
    private EditText numImagesInput;
    private Button uploadButton, submitButton;
    private List<Uri> imageUris = new ArrayList<>();
    private List<String> imageNames = new ArrayList<>();
    private Map<String, String> selectedCategories = new HashMap<>();
    private Map<String, String> selectedColors = new HashMap<>();
    private ApiService apiService;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image_analysis, container, false);

        imagePreviewLayout = root.findViewById(R.id.imagePreviewLayout);
        backendImageLayout = root.findViewById(R.id.backendImageLayout);
        uploadButton = root.findViewById(R.id.uploadButton);
        submitButton = root.findViewById(R.id.submitButton);
        numImagesInput = root.findViewById(R.id.numImagesInput);

        // Request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        uploadButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> sendImagesToBackend());

        return root;
    }

    /**
     * Opens the file picker instead of just the gallery.
     * Users can now select images from Downloads, Google Drive, and other locations.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }
            displayImages();
        }
    }

    private void displayImages() {
        imagePreviewLayout.removeAllViews();
        for (Uri uri : imageUris) {
            ImageView imgView = new ImageView(getActivity());
            imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            Glide.with(getActivity()).load(uri).into(imgView);  // Use Glide for async loading
            imagePreviewLayout.addView(imgView);
        }
    }

    private void sendImagesToBackend() {
        Executors.newSingleThreadExecutor().execute(() -> {  // Run in background
            List<MultipartBody.Part> imageParts = new ArrayList<>();
            for (Uri uri : imageUris) {
                File file = new File(uri.getPath());
                RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("images", file.getName(), requestBody);
                imageParts.add(imagePart);
            }

            apiService.analyzeImages(imageParts).enqueue(new Callback<List<Map<String, String>>>() {
                @Override
                public void onResponse(Call<List<Map<String, String>>> call, Response<List<Map<String, String>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        requireActivity().runOnUiThread(() -> {
                            backendImageLayout.removeAllViews();
                            for (Map<String, String> imageResponse : response.body()) {
                                displayImageResult(imageResponse);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, String>>> call, Throwable t) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
    }

    private void displayImageResult(Map<String, String> imageResponse) {
        String imageName = imageResponse.get("image_name");
        String category = imageResponse.get("category");
        String color = imageResponse.get("color");

        imageNames.add(imageName);

        // Create Image View
        ImageView imgView = new ImageView(getActivity());
        imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        Glide.with(getActivity()).load(imageResponse.get("image_url")).into(imgView);

        // Category and Color Display
        TextView categoryView = new TextView(getActivity());
        categoryView.setText("Category: " + category);

        TextView colorView = new TextView(getActivity());
        colorView.setText("Color: " + color);

        // Create Layout for Image + Info
        LinearLayout imageContainer = new LinearLayout(getActivity());
        imageContainer.setOrientation(LinearLayout.VERTICAL);
        imageContainer.addView(imgView);
        imageContainer.addView(categoryView);
        imageContainer.addView(colorView);

        backendImageLayout.addView(imageContainer);
    }
}
