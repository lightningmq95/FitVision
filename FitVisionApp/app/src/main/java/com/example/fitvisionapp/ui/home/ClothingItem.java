package com.example.fitvisionapp.ui.home;
import android.graphics.Bitmap;

public class ClothingItem {
    private final String name;
    private final String category;
    //private final int imageResId;
    private final Bitmap imageBitmap;

    public ClothingItem(String name, String category, Bitmap imageBitmap) {
        this.name = name;
        this.category = category;
        this.imageBitmap = imageBitmap;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }
}
