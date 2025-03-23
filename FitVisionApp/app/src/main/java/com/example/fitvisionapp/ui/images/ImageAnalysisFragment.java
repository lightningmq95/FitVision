//package com.example.fitvisionapp.ui.image;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.fitvisionapp.R;
//
//import java.io.IOException;
//
//
//public class ImageAnalysisFragment extends Fragment {
//
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private ImageView imageView;
//    private TextView categoryTextView, colorTextView;
//    private Uri imageUri;
//
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View root = inflater.inflate(R.layout.fragment_image_analysis, container, false);
//
//        imageView = root.findViewById(R.id.imageView);
//        Button uploadButton = root.findViewById(R.id.uploadButton);
//        categoryTextView = root.findViewById(R.id.categoryTextView);
//        colorTextView = root.findViewById(R.id.colorTextView);
//        Button correctButton = root.findViewById(R.id.correctButton);
//        Button incorrectButton = root.findViewById(R.id.incorrectButton);
//
//        // Open image picker
//        uploadButton.setOnClickListener(v -> openImagePicker());
//
//        // Correct button action
//        correctButton.setOnClickListener(v -> {
//            Toast.makeText(getActivity(), "Marked as Correct", Toast.LENGTH_SHORT).show();
//        });
//
//        // Incorrect button action
//        incorrectButton.setOnClickListener(v -> {
//            Toast.makeText(getActivity(), "Marked as Incorrect", Toast.LENGTH_SHORT).show();
//        });
//
//        return root;
//    }
//
//    // Open gallery to select image
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
//            imageUri = data.getData();
//            try {
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
//                imageView.setImageBitmap(bitmap);
//
//                // Simulating category & color detection
//                categoryTextView.setText("Category: T-Shirt");
//                colorTextView.setText("Color: Blue");
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}

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

    private static final int PICK_IMAGE_REQUEST = 1;
    private LinearLayout imagePreviewLayout, backendImageLayout;
    private TextView categoryTextView, colorTextView;
    private EditText usernameInput, numImagesInput;
    private Button uploadButton, submitButton, confirmCorrectionButton;
    private List<Uri> imageUris = new ArrayList<>();
    private List<String> imageNames = new ArrayList<>();
    private Map<String, String> selectedCategories = new HashMap<>();
    private Map<String, String> selectedColors = new HashMap<>();
    private ApiService apiService;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image_analysis, container, false);

        // Initialize UI components
        imagePreviewLayout = root.findViewById(R.id.imagePreviewLayout);
        backendImageLayout = root.findViewById(R.id.backendImageLayout);
        uploadButton = root.findViewById(R.id.uploadButton);
        submitButton = root.findViewById(R.id.submitButton);
        confirmCorrectionButton = root.findViewById(R.id.confirmCorrectionButton);
        categoryTextView = root.findViewById(R.id.categoryTextView);
        colorTextView = root.findViewById(R.id.colorTextView);
        usernameInput = root.findViewById(R.id.usernameInput);
        numImagesInput = root.findViewById(R.id.numImagesInput);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/") // Localhost for Android Emulator
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Image Picker
        uploadButton.setOnClickListener(v -> openImagePicker());

        // Submit Data
        submitButton.setOnClickListener(v -> sendImagesToBackend());

        // Confirm Corrections
        confirmCorrectionButton.setOnClickListener(v -> sendCorrectionToBackend());

        return root;
    }

    private void openImagePicker() {
        int numImages;
        try {
            numImages = Integer.parseInt(numImagesInput.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Enter a valid number of images", Toast.LENGTH_SHORT).show();
            return;
        }

        if (numImages <= 0) {
            Toast.makeText(getActivity(), "Number of images must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        imageUris.clear();
        for (int i = 0; i < numImages; i++) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
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
        if (imageUris.isEmpty() || usernameInput.getText().toString().isEmpty()) {
            Toast.makeText(getActivity(), "Please upload images and enter username", Toast.LENGTH_SHORT).show();
            return;
        }

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : imageUris) {
            File file = new File(uri.getPath());
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestBody);
            imageParts.add(imagePart);
        }

        RequestBody usernamePart = RequestBody.create(MediaType.parse("text/plain"), usernameInput.getText().toString());

        apiService.analyzeImages(usernamePart, imageParts).enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(Call<List<Map<String, String>>> call, Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    backendImageLayout.removeAllViews();

                    for (Map<String, String> imageResponse : response.body()) {
                        String imageUrl = imageResponse.get("image_url");
                        String category = imageResponse.get("category");
                        String color = imageResponse.get("color");
                        String imageName = imageResponse.get("image_name");

                        imageNames.add(imageName);

                        // Create ImageView for each response
                        ImageView imgView = new ImageView(getActivity());
                        imgView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
                        Glide.with(getActivity()).load(imageUrl).into(imgView);

                        TextView categoryView = new TextView(getActivity());
                        categoryView.setText("Category: " + category);
                        TextView colorView = new TextView(getActivity());
                        colorView.setText("Color: " + color);

                        Button correctBtn = new Button(getActivity());
                        correctBtn.setText("Correct");

                        Button incorrectBtn = new Button(getActivity());
                        incorrectBtn.setText("Incorrect");

                        Spinner clothingTypeSpinner = new Spinner(getActivity());
                        Spinner colorSpinner = new Spinner(getActivity());

                        incorrectBtn.setOnClickListener(v -> {
                            clothingTypeSpinner.setVisibility(View.VISIBLE);
                            colorSpinner.setVisibility(View.VISIBLE);
                            confirmCorrectionButton.setVisibility(View.VISIBLE);
                        });

                        confirmCorrectionButton.setOnClickListener(v -> {
                            selectedCategories.put(imageName, clothingTypeSpinner.getSelectedItem().toString());
                            selectedColors.put(imageName, colorSpinner.getSelectedItem().toString());
                        });

                        LinearLayout imageContainer = new LinearLayout(getActivity());
                        imageContainer.setOrientation(LinearLayout.VERTICAL);
                        imageContainer.addView(imgView);
                        imageContainer.addView(categoryView);
                        imageContainer.addView(colorView);
                        imageContainer.addView(correctBtn);
                        imageContainer.addView(incorrectBtn);
                        imageContainer.addView(clothingTypeSpinner);
                        imageContainer.addView(colorSpinner);

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

    private void sendCorrectionToBackend() {
        List<Map<String, String>> correctionList = new ArrayList<>();

        for (String imageName : imageNames) {
            if (selectedCategories.containsKey(imageName) && selectedColors.containsKey(imageName)) {
                Map<String, String> data = new HashMap<>();
                data.put("image_name", imageName);
                data.put("category", selectedCategories.get(imageName));
                data.put("color", selectedColors.get(imageName));
                correctionList.add(data);
            }
        }

        apiService.sendCorrections(correctionList).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Toast.makeText(getActivity(), "Corrections Sent!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
