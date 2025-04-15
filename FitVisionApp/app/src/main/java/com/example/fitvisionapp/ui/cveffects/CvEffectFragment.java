package com.example.fitvisionapp.ui.cveffects;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;
import com.example.fitvisionapp.network.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.*;

public class CvEffectFragment extends Fragment {

    private ImageView imagePreview, resultImage, bgPreview;
    private EditText inputMode;
    private Button btnUser, btnBG, btnApply;

    private String userImageBase64 = "";
    private String bgImageBase64 = "";

    private static final int USER_IMG = 1;
    private static final int BG_IMG = 2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cv_effect, container, false);

        imagePreview = root.findViewById(R.id.imagePreview);
        bgPreview = root.findViewById(R.id.bgPreview);
        resultImage = root.findViewById(R.id.resultImage);
        inputMode = root.findViewById(R.id.inputMode);
        btnUser = root.findViewById(R.id.btnSelectUserImage);
        btnBG = root.findViewById(R.id.btnSelectBGImage);
        btnApply = root.findViewById(R.id.btnApplyEffect);

        btnUser.setOnClickListener(v -> selectImage(USER_IMG));
        btnBG.setOnClickListener(v -> selectImage(BG_IMG));
        btnApply.setOnClickListener(v -> sendToBackend());

        return root;
    }

    private void selectImage(int code) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Select Picture"), code);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if (resCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (InputStream is = getContext().getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                String base64 = encodeImage(bitmap);

                if (reqCode == USER_IMG) {
                    userImageBase64 = base64;
                    imagePreview.setImageBitmap(bitmap);
                } else if (reqCode == BG_IMG) {
                    bgImageBase64 = base64;
                    bgPreview.setImageBitmap(bitmap); // show background image
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToBackend() {
        String mode = inputMode.getText().toString().trim();
        if (userImageBase64.isEmpty() || mode.isEmpty()) {
            Toast.makeText(getContext(), "Missing input!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mode.equalsIgnoreCase("bgchange") && bgImageBase64.isEmpty()) {
            Toast.makeText(getContext(), "Select a background image", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, String> map = new HashMap<>();
        map.put("mode", mode);
        map.put("image", userImageBase64);
        if (mode.equalsIgnoreCase("bgchange")) {
            map.put("background", bgImageBase64);
        }

        ApiService api = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        api.cvTransform(map).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> res) {
                try {
                    byte[] img = res.body().bytes();
                    Bitmap bmp = BitmapFactory.decodeByteArray(img, 0, img.length);
                    resultImage.setImageBitmap(bmp);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Decode error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Connecting!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] imageBytes = bos.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }
}
