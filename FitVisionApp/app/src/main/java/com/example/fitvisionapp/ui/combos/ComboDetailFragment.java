package com.example.fitvisionapp.ui.combos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.fitvisionapp.R;
import com.example.fitvisionapp.models.ComboImage;
import com.example.fitvisionapp.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ComboDetailFragment extends Fragment {

    private LinearLayout imageContainer;
    private ApiService apiService;
    private String userId, comboName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_combo_detail, container, false);
        imageContainer = root.findViewById(R.id.imageContainer);
        TextView backButton = root.findViewById(R.id.backButton);

        // Get Data
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            comboName = args.getString("comboName");
        }

        // Initialize API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")  // Change if backend is hosted online
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        fetchComboDetails();

        // Back button functionality
        backButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        return root;
    }

    private void fetchComboDetails() {
        apiService.getComboDetails(userId, comboName).enqueue(new Callback<List<ComboImage>>() {
            @Override
            public void onResponse(Call<List<ComboImage>> call, Response<List<ComboImage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (ComboImage item : response.body()) {
                        View imageViewLayout = LayoutInflater.from(getActivity()).inflate(R.layout.item_combo_image, imageContainer, false);
                        ImageView imageView = imageViewLayout.findViewById(R.id.imageView);
                        TextView categoryText = imageViewLayout.findViewById(R.id.categoryText);

                        categoryText.setText(item.category);

                        if (item.imageData != null && !item.imageData.isEmpty()) {
                            if (item.imageData.startsWith("http")) {
                                // Load from URL
                                Glide.with(getActivity()).load(item.imageData).into(imageView);
                            } else {
                                // Load from Base64 safely
                                try {
                                    byte[] decodedString = Base64.decode(item.imageData, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    imageView.setImageBitmap(decodedByte);
                                } catch (IllegalArgumentException e) {
                                    Toast.makeText(getActivity(), "Invalid image data!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            // Set placeholder image if imageData is missing
                            imageView.setImageResource(android.R.drawable.ic_menu_gallery);

                        }

                        imageContainer.addView(imageViewLayout);
                    }
                } else {
                    Toast.makeText(getActivity(), "Failed to load combo!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ComboImage>> call, Throwable t) {
                Toast.makeText(getActivity(), "Connection failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
