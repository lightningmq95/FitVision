package com.fitvision.app.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import com.fitvision.app.R;

public class GalleryLayoutOneFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gallery_layout_one, container, false);
        Button uploadButton = view.findViewById(R.id.upload);
        TextView textView = view.findViewById(R.id.textView2);
        Button button = view.findViewById(R.id.submit);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadButton.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                button.setVisibility(View.GONE);

                FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_containers, new GalleryLayoutTwoFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return view;    }
}