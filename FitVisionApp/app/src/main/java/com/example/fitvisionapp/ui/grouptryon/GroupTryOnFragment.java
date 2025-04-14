package com.example.fitvisionapp.ui.grouptryon;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;
import com.example.fitvisionapp.network.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.*;

public class GroupTryOnFragment extends Fragment {

    private ImageView inputImage, resultImage;
    private LinearLayout facesContainer;
    private Button uploadBtn, backBtn;
    private Bitmap uploadedBitmap;
    private String base64Input;

    private static final int IMAGE_PICK_REQUEST = 100;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_group_tryon, container, false);

        inputImage = root.findViewById(R.id.inputImage);
        resultImage = root.findViewById(R.id.resultImage);
        facesContainer = root.findViewById(R.id.facesContainer);
        uploadBtn = root.findViewById(R.id.btnUpload);
        backBtn = root.findViewById(R.id.btnBack);

        uploadBtn.setOnClickListener(v -> openGallery());
        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        return root;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (reqCode == IMAGE_PICK_REQUEST && resCode == Activity.RESULT_OK && data != null) {
            try {
                InputStream stream = requireActivity().getContentResolver().openInputStream(data.getData());
                uploadedBitmap = BitmapFactory.decodeStream(stream);
                inputImage.setImageBitmap(uploadedBitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                uploadedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                base64Input = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                sendImageForFaceDetection(base64Input);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendImageForFaceDetection(String base64Image) {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.detectFaces(base64Image).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    displayFaceThumbnails(response.body());
                } else {
                    Toast.makeText(getContext(), "Face detection failed!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(getContext(), "API error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayFaceThumbnails(List<String> faceBase64List) {
        facesContainer.removeAllViews();

        for (String base64Face : faceBase64List) {
            byte[] decoded = Base64.decode(base64Face, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

            ImageView faceView = new ImageView(getContext());
            faceView.setImageBitmap(bitmap);
            faceView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            faceView.setPadding(10, 10, 10, 10);
            faceView.setOnClickListener(v -> sendSelectedFace(base64Face));

            facesContainer.addView(faceView);
        }
    }

    private void sendSelectedFace(String selectedFaceBase64) {
        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.tryOnSelectedFace(selectedFaceBase64).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    byte[] imageBytes = response.body().bytes();
                    Bitmap result = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    resultImage.setImageBitmap(result);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Failed to process try-on!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Try-on API failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
