package com.example.fitvisionapp.ui.combos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ComboGridFragment extends Fragment {

    private GridView gridView;
    private ApiService apiService;
    private String userId;
    private ComboGridAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_combo_grid, container, false);
        gridView = root.findViewById(R.id.comboGridView);

        //  Bypass Firebase Login for Testing
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            userId = "devastatingrpg";  //  Use test ID if not logged in
            Toast.makeText(getActivity(), "WARNING: Using Test User ID!", Toast.LENGTH_SHORT).show();
        } else {
            userId = user.getUid();  //  Use real ID if logged in
        }

        // Setup HTTP client with longer timeout
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Initialize API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.10:8000/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        fetchCombos();

        return root;
    }

    private void fetchCombos() {
        apiService.getCombos().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter = new ComboGridAdapter(getActivity(), response.body());
                    gridView.setAdapter(adapter);

                    // Click event
                    gridView.setOnItemClickListener((parent, view, position, id) -> {
                        String comboName = adapter.getItem(position);
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", userId);
                        bundle.putString("comboName", comboName);
                        Navigation.findNavController(view).navigate(R.id.action_comboGrid_to_comboDetail, bundle);
                    });
                } else {
                    Toast.makeText(getActivity(), "Failed to load combos!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(getActivity(), "Connection failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
