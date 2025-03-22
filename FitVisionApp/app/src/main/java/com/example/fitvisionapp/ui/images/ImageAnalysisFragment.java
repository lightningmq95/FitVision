package com.example.fitvisionapp.ui.image;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitvisionapp.R;

import java.io.IOException;


public class ImageAnalysisFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private TextView categoryTextView, colorTextView;
    private Uri imageUri;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image_analysis, container, false);

        imageView = root.findViewById(R.id.imageView);
        Button uploadButton = root.findViewById(R.id.uploadButton);
        categoryTextView = root.findViewById(R.id.categoryTextView);
        colorTextView = root.findViewById(R.id.colorTextView);
        Button correctButton = root.findViewById(R.id.correctButton);
        Button incorrectButton = root.findViewById(R.id.incorrectButton);

        // Open image picker
        uploadButton.setOnClickListener(v -> openImagePicker());

        // Correct button action
        correctButton.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Marked as Correct", Toast.LENGTH_SHORT).show();
        });

        // Incorrect button action
        incorrectButton.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Marked as Incorrect", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    // Open gallery to select image
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);

                // Simulating category & color detection
                categoryTextView.setText("Category: T-Shirt");
                colorTextView.setText("Color: Blue");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
