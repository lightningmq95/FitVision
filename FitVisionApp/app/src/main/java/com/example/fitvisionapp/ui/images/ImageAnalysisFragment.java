package com.example.fitvisionapp.ui.images;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
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

public class ImageAnalysisFragment extends Fragment {

    private LinearLayout imagePreviewLayout;
    private Button uploadButton, submitButton, correctButton, incorrectButton, confirmCorrectionButton;
    private Spinner clothingTypeSpinner, colorSpinner;
    private EditText clothingNameInput; // NEW: Clothing name input
    private Uri selectedImageUri;
    private ApiService apiService;
    private TextView categoryText, colorText;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image_analysis, container, false);

        imagePreviewLayout = root.findViewById(R.id.imagePreviewLayout);
        uploadButton = root.findViewById(R.id.uploadButton);
        submitButton = root.findViewById(R.id.submitButton);
        correctButton = root.findViewById(R.id.correctButton);
        incorrectButton = root.findViewById(R.id.incorrectButton);
        confirmCorrectionButton = root.findViewById(R.id.confirmCorrectionButton);
        clothingTypeSpinner = root.findViewById(R.id.clothingTypeSpinner);
        colorSpinner = root.findViewById(R.id.colorSpinner);
        categoryText = root.findViewById(R.id.categoryText);
        colorText = root.findViewById(R.id.colorText);
        clothingNameInput = root.findViewById(R.id.clothingNameInput); // NEW: Initializing clothing name field

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")  // Update with actual backend URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        uploadButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> sendImageToBackend());
        correctButton.setOnClickListener(v -> Toast.makeText(getActivity(), "Data Confirmed!", Toast.LENGTH_SHORT).show());
        incorrectButton.setOnClickListener(v -> showCorrectionFields());
        confirmCorrectionButton.setOnClickListener(v -> sendCorrectedData());

        return root;
    }

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

    private void displayImage() {
        imagePreviewLayout.removeAllViews();
        ImageView imgView = new ImageView(getActivity());
        imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        Glide.with(getActivity()).load(selectedImageUri).into(imgView);
        imagePreviewLayout.addView(imgView);
    }

    private String getRealPathFromURI(Uri uri) {
        if (getActivity() == null) return null;
        String filePath = null;

        if (DocumentsContract.isDocumentUri(getActivity(), uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if (uri.getAuthority().equals("com.android.providers.media.documents")) {
                String id = docId.split(":")[1];
                String[] columns = {MediaStore.Images.Media.DATA};
                String selection = MediaStore.Images.Media._ID + "=?";
                filePath = queryFilePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, selection, new String[]{id});
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            filePath = queryFilePath(uri, new String[]{MediaStore.Images.Media.DATA}, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
        }

        return filePath;
    }

    private String queryFilePath(Uri uri, String[] projection, String selection, String[] selectionArgs) {
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                String path = cursor.getString(columnIndex);
                cursor.close();
                return path;
            }
            cursor.close();
        }
        return null;
    }

    private void sendImageToBackend() {
        if (selectedImageUri == null) {
            Toast.makeText(getActivity(), "No image selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        String clothingName = clothingNameInput.getText().toString().trim(); // NEW: Get clothing name input
        if (clothingName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter clothing name!", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath = getRealPathFromURI(selectedImageUri);
        if (imagePath == null) {
            Toast.makeText(getActivity(), "Could not get image path!", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(imagePath);
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestBody);
        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), clothingName); // NEW: Clothing name request body

        apiService.analyzeImage(imagePart, namePart).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> result = response.body();
                    categoryText.setText("Category: " + result.get("category"));
                    colorText.setText("Color: " + result.get("color"));

                    categoryText.setVisibility(View.VISIBLE);
                    colorText.setVisibility(View.VISIBLE);
                    correctButton.setVisibility(View.VISIBLE);
                    incorrectButton.setVisibility(View.VISIBLE);
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

    private void showCorrectionFields() {
        clothingTypeSpinner.setVisibility(View.VISIBLE);
        colorSpinner.setVisibility(View.VISIBLE);
        confirmCorrectionButton.setVisibility(View.VISIBLE);
    }

    private void sendCorrectedData() {
        Map<String, String> correctedData = new HashMap<>();
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
