package com.fitvision.app.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.fitvision.app.R;
import com.fitvision.app.databinding.FragmentGalleryBinding;

public class GalleryFragment extends Fragment {

    private static final int PICK_FILE_REQUEST = 1;

    private FragmentGalleryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textView2;
        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Button uploadButton = binding.upload;
        uploadButton.setOnClickListener(v -> openFilePicker());

        Button submitButton = binding.submit;
        submitButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new OutputFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });
        return root;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}