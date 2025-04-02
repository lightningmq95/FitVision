package com.example.fitvisionapp.ui.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.databinding.FragmentHomeBinding;
import com.example.fitvisionapp.network.ApiService;
import com.example.fitvisionapp.network.ApiImageTemp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.fitvisionapp.network.RetrofitClient;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ClothingAdapter adapter;
    private List<ClothingItem> allItems = new ArrayList<>();

    private static final String BASE_URL = "http://10.0.2.2:5000/";
    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewGrid;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        String userId = getActivity().getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = "test_user"; // Default user for testing
        }

        adapter = new ClothingAdapter(getContext(), new ArrayList<>(), userId);
        recyclerView.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.clothing_filters, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Spinner selected category: " + selectedCategory);
                applyFilter(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Load Dummy Data for Testing
        loadDummyData();

        // Uncomment this to fetch from the API
        // fetchClothingItemsFromApi();

        return root;
    }

    // Load Dummy Data for Testing
    private void loadDummyData() {
        List<ClothingItem> dummyItems = new ArrayList<>();

        String dummyBase64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgAB";
        Bitmap dummyBitmap = decodeBase64ToBitmap(dummyBase64Image);

        dummyItems.add(new ClothingItem("Shirt 1", "Shirt", dummyBitmap));
        dummyItems.add(new ClothingItem("Pants 1", "Pants", dummyBitmap));
        dummyItems.add(new ClothingItem("Jacket 1", "Jacket", dummyBitmap));
        dummyItems.add(new ClothingItem("Shoes 1", "Shoes", dummyBitmap));

        allItems = dummyItems;
        adapter.updateData(new ArrayList<>(allItems));

        Log.d(TAG, "Loaded dummy data with " + allItems.size() + " items.");
    }

    private void fetchClothingItemsFromApi() {
        Log.d(TAG, "Starting API request...");

        // Setup HTTP logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)  // Attach logging
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // Change the call to expect a List<ApiImageTemp>
        Call<List<ApiImageTemp>> call = apiService.getImages();

        call.enqueue(new Callback<List<ApiImageTemp>>() {
            @Override
            public void onResponse(@NonNull Call<List<ApiImageTemp>> call, @NonNull Response<List<ApiImageTemp>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiImageTemp> apiImages = response.body();
                    Log.d(TAG, "API call successful. Received " + apiImages.size() + " images.");

                    List<ClothingItem> tempList = new ArrayList<>();
                    for (ApiImageTemp apiImage : apiImages) {
                        Log.d(TAG, "Processing image: " + apiImage.getImage_name());
                        Bitmap bitmap = decodeBase64ToBitmap(apiImage.getImage_data());
                        if (bitmap == null) {
                            Log.e(TAG, "Failed to decode image: " + apiImage.getImage_name());
                        } else {
                            Log.d(TAG, "Image decoded successfully: " + apiImage.getImage_name());
                        }
                        String categoryStr = convertCategoryIntToString(apiImage.getCategory());
                        ClothingItem item = new ClothingItem(apiImage.getImage_name(), categoryStr, bitmap);
                        tempList.add(item);
                    }

                    allItems = tempList;
                    requireActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Updating adapter with " + allItems.size() + " items.");
                        adapter.updateData(new ArrayList<>(allItems));
                    });
                } else {
                    Log.e(TAG, "API call unsuccessful: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ApiImageTemp>> call, @NonNull Throwable t) {
                Log.e(TAG, "API request failed", t);
            }
        });
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            if (base64Str.startsWith("data:")) {
                base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
            }
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode base64 image", e);
            return null;
        }
    }

    private String convertCategoryIntToString(int cat) {
        switch (cat) {
            case 0:
                return "Hat";
            case 1:
                return "Shirt";
            case 2:
                return "Pants";
            case 3:
                return "Jacket";
            case 4:
                return "Shoes";
            case 5:
                return "Shorts";
            default:
                return "Other";
        }
    }

    private void applyFilter(String category) {
        List<ClothingItem> filtered = new ArrayList<>();
        if (category.equalsIgnoreCase("All")) {
            filtered = new ArrayList<>(allItems);
        } else {
            for (ClothingItem item : allItems) {
                if (item.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(item);
                }
            }
        }
        Log.d(TAG, "Filtered " + filtered.size() + " items for category: " + category);
        adapter.updateData(filtered);
    }

    private void sendAddToComboRequest(String userId, String comboName, String clothingId, String category) {
        HashMap<String, String> requestData = new HashMap<>();
        requestData.put("userId", userId);
        requestData.put("comboName", comboName);
        requestData.put("clothingId", clothingId);
        requestData.put("category", category);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<ResponseBody> call = apiService.addToCombo(requestData);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, response.isSuccessful() ? "Item added to combo!" : "Failed: " + response.code());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Request failed", t);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
