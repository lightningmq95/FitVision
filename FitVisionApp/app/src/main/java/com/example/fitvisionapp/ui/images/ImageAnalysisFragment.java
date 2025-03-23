package com.example.fitvisionapp.ui.images;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;

import java.io.File;
import java.util.*;

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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        uploadButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> sendImagesToBackend());

        return root;
    }

    private void openImagePicker() {
        int numImages = Integer.parseInt(numImagesInput.getText().toString());
        imageUris.clear();
        for (int i = 0; i < numImages; i++) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            imageUris.add(imageUri);
            displayImages();
        }
    }

    private void displayImages() {
        imagePreviewLayout.removeAllViews();
        for (Uri uri : imageUris) {
            ImageView imgView = new ImageView(getActivity());
            imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            imgView.setImageURI(uri);
            imagePreviewLayout.addView(imgView);
        }
    }

    private void sendImagesToBackend() {
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
                    backendImageLayout.removeAllViews();
                    for (Map<String, String> imageResponse : response.body()) {
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

                        // Correct / Incorrect Buttons
                        Button correctButton = new Button(getActivity());
                        correctButton.setText("Correct");

                        Button incorrectButton = new Button(getActivity());
                        incorrectButton.setText("Incorrect");

                        // Dropdowns (Hidden by Default)
                        Spinner clothingTypeSpinner = new Spinner(getActivity());
                        ArrayAdapter<CharSequence> clothingAdapter = ArrayAdapter.createFromResource(
                                getActivity(), R.array.clothing_types, android.R.layout.simple_spinner_item);
                        clothingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        clothingTypeSpinner.setAdapter(clothingAdapter);
                        clothingTypeSpinner.setVisibility(View.GONE);

                        Spinner colorSpinner = new Spinner(getActivity());
                        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(
                                getActivity(), R.array.color_options, android.R.layout.simple_spinner_item);
                        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        colorSpinner.setAdapter(colorAdapter);
                        colorSpinner.setVisibility(View.GONE);

                        Button confirmCorrectionButton = new Button(getActivity());
                        confirmCorrectionButton.setText("Confirm Correction");
                        confirmCorrectionButton.setVisibility(View.GONE);

                        // Handle Incorrect Button Click
                        incorrectButton.setOnClickListener(v -> {
                            clothingTypeSpinner.setVisibility(View.VISIBLE);
                            colorSpinner.setVisibility(View.VISIBLE);
                            confirmCorrectionButton.setVisibility(View.VISIBLE);
                        });

                        // Handle Confirm Correction Click
                        confirmCorrectionButton.setOnClickListener(v -> {
                            selectedCategories.put(imageName, clothingTypeSpinner.getSelectedItem().toString());
                            selectedColors.put(imageName, colorSpinner.getSelectedItem().toString());
                        });

                        // Create Layout for Image + Buttons
                        LinearLayout imageContainer = new LinearLayout(getActivity());
                        imageContainer.setOrientation(LinearLayout.VERTICAL);
                        imageContainer.addView(imgView);
                        imageContainer.addView(categoryView);
                        imageContainer.addView(colorView);
                        imageContainer.addView(correctButton);
                        imageContainer.addView(incorrectButton);
                        imageContainer.addView(clothingTypeSpinner);
                        imageContainer.addView(colorSpinner);
                        imageContainer.addView(confirmCorrectionButton);

                        backendImageLayout.addView(imageContainer);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, String>>> call, Throwable t) {
                Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
