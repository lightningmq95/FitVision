package com.example.fitvisionapp.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitvisionapp.R;
import com.example.fitvisionapp.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {

    private final List<ClothingItem> items;
    private final Context context;
    private final String userId;
    private AlertDialog currentDialog;

    public ClothingAdapter(Context context, List<ClothingItem> items, String userId) {
        this.context = context;
        this.items = items;
        this.userId = userId;
    }

    @NonNull
    @Override
    public ClothingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid, parent, false);
        return new ClothingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClothingViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        holder.imageView.setImageBitmap(item.getImageBitmap());
        holder.textView.setText(item.getCategory());

        holder.itemView.setOnClickListener(v -> showPopup(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<ClothingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_clothing);
            textView = itemView.findViewById(R.id.text_clothing_name);
        }
    }

    // Show Popup Dialog
    private void showPopup(ClothingItem item) {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_to_combo, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        currentDialog = dialog;

        Button yesButton = dialogView.findViewById(R.id.yesButton);
        Button noButton = dialogView.findViewById(R.id.noButton);
        TextView popupText = dialogView.findViewById(R.id.popupText);

        popupText.setText("Do you want to add this item to a combo?");

        yesButton.setOnClickListener(v -> {
            sendAddToComboRequest(item);
            dialog.dismiss();
        });

        noButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void sendAddToComboRequest(ClothingItem item) {
        String comboName = "Casual Outfit";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<Void> call = apiService.addToCombo(userId, comboName, item.getName(), item.getCategory());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Added to combo successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to add to combo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Connection failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
