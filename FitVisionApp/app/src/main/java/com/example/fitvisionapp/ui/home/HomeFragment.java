package com.example.fitvisionapp.ui.home;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ClothingAdapter adapter;
    private List<ClothingItem> allItems;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewGrid;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        allItems = new ArrayList<>();

        allItems.add(new ClothingItem("T-Shirt 1", "Shirt", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("T-Shirt 2", "Shirt", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Pants 1", "Pants", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Pants 2", "Pants", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 1", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 2", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Jacket 1", "Jacket", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Jacket 2", "Jacket", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shoes 1", "Shoes", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shoes 2", "Shoes", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shorts 1", "Shorts", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shorts 2", "Shorts", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("T-Shirt 3", "Shirt", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Pants 3", "Pants", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 3", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 6", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 2", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("T-Shirt 1", "Shirt", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("T-Shirt 2", "Shirt", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Pants 1", "Pants", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Pants 2", "Pants", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 1", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 2", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Jacket 1", "Jacket", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Jacket 2", "Jacket", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shoes 1", "Shoes", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shoes 2", "Shoes", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shorts 1", "Shorts", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Shorts 2", "Shorts", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("T-Shirt 3", "Shirt", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Pants 3", "Pants", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 3", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 6", "Hat", R.drawable.ic_launcher_foreground));
        allItems.add(new ClothingItem("Hat 2", "Hat", R.drawable.ic_launcher_foreground));

        adapter = new ClothingAdapter(new ArrayList<>(allItems));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.clothing_filters,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = parent.getItemAtPosition(position).toString();
                applyFilter(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // binding.buttonApplyFilter.setOnClickListener(v -> {
        //     String selectedCategory = binding.spinnerFilter.getSelectedItem().toString();
        //     applyFilter(selectedCategory);
        // });

        return root;
    }

    private void applyFilter(String category) {
        if (category.equals("All")) {
            adapter.updateData(new ArrayList<>(allItems));
        } else {
            List<ClothingItem> filtered = new ArrayList<>();
            for (ClothingItem item : allItems) {
                if (item.getCategory().equals(category)) {
                    filtered.add(item);
                }
            }
            adapter.updateData(filtered);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
