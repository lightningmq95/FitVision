package com.example.fitvisionapp.ui.images;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImageAnalysisFragment extends Fragment {

    private LinearLayout imagePreviewLayout, correctIncorrectLayout;
    private Button uploadButton, submitButton, correctButton, incorrectButton, confirmCorrectionButton;
    private Spinner clothingTypeSpinner, colorSpinner;
    private EditText clothingNameInput;
    private Uri selectedImageUri;
    private ApiService apiService;
    private TextView categoryText, colorText;
    private String imageId, userId;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image_analysis, container, false);

        // ✅ Firebase User ID Check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            userId = "devastatingrpg";  // TEMPORARY TEST VALUE
            Toast.makeText(getActivity(), "WARNING: Using Test User ID!", Toast.LENGTH_SHORT).show();
        } else {
            userId = user.getUid();
        }

        // UI Elements
        imagePreviewLayout = root.findViewById(R.id.imagePreviewLayout);
        uploadButton = root.findViewById(R.id.uploadButton);
        submitButton = root.findViewById(R.id.submitButton);
        correctButton = root.findViewById(R.id.correctButton);
        incorrectButton = root.findViewById(R.id.incorrectButton);
        confirmCorrectionButton = root.findViewById(R.id.confirmCorrectionButton);
        clothingTypeSpinner = root.findViewById(R.id.clothingTypeSpinner);
        colorSpinner = root.findViewById(R.id.colorSpinner);
        clothingNameInput = root.findViewById(R.id.clothingNameInput);
        categoryText = root.findViewById(R.id.categoryText);
        colorText = root.findViewById(R.id.colorText);
        correctIncorrectLayout = root.findViewById(R.id.correctIncorrectLayout);

        // Hide initially
        categoryText.setVisibility(View.GONE);
        colorText.setVisibility(View.GONE);
        correctIncorrectLayout.setVisibility(View.GONE);
        clothingTypeSpinner.setVisibility(View.GONE);
        colorSpinner.setVisibility(View.GONE);
        confirmCorrectionButton.setVisibility(View.GONE);

        // Setup HTTP client with longer timeout
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Retrofit API Service
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Click Listeners
        uploadButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> sendImageToBackend());
        incorrectButton.setOnClickListener(v -> showCorrectionFields());
        confirmCorrectionButton.setOnClickListener(v -> sendCorrectedData());

        return root;
    }

    // Open Gallery to Pick Image
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            displayImage();
        }
    }

    // Display Image in Preview
    private void displayImage() {
        imagePreviewLayout.removeAllViews();
        ImageView imgView = new ImageView(getActivity());
        imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        Glide.with(getActivity()).load(selectedImageUri).into(imgView);
        imagePreviewLayout.addView(imgView);
    }

    // Convert URI to File Path
    private String getRealPathFromURI(Uri uri) {
        if (getActivity() == null) return null;
        String filePath = null;

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath;
    }

    // Send Image to Backend
    private void sendImageToBackend() {
        String clothingName = clothingNameInput.getText().toString();
        if (selectedImageUri == null) {
            Toast.makeText(getActivity(), "No image selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath = getRealPathFromURI(selectedImageUri);
        if (imagePath == null) {
            Toast.makeText(getActivity(), "Could not get image path!", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(imagePath);
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), clothingName);

        apiService.uploadImage(userId, imagePart, namePart).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> result = response.body();
                    imageId = result.get("image_id");
                    categoryText.setText("Category: " + result.get("category"));
//                    colorText.setText("Color: " + result.get("color"));

                    categoryText.setVisibility(View.VISIBLE);
//                    colorText.setVisibility(View.VISIBLE);
                    correctIncorrectLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), "Server Error!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(getActivity(), "Failed to connect!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Show Dropdowns When "Incorrect" is Clicked
    private void showCorrectionFields() {
        clothingTypeSpinner.setVisibility(View.VISIBLE);
        colorSpinner.setVisibility(View.VISIBLE);
        confirmCorrectionButton.setVisibility(View.VISIBLE);
    }

    // Send Corrected Data to Backend
    private void sendCorrectedData() {
        Map<String, String> correctedData = new HashMap<>();
        correctedData.put("userid", userId);
        correctedData.put("image_id", imageId);
        correctedData.put("corrected_category", clothingTypeSpinner.getSelectedItem().toString());
        correctedData.put("corrected_color", colorSpinner.getSelectedItem().toString());

        apiService.sendCorrection(correctedData).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getActivity(), "Correction Sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Error sending correction!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(getActivity(), "Connection Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
