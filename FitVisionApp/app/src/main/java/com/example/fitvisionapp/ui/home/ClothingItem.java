package com.example.fitvisionapp.ui.home;

public class ClothingItem {
    private final String name;
    private final String category;
    private final int imageResId;

    public ClothingItem(String name, String category, int imageResId) {
        this.name = name;
        this.category = category;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getImageResId() {
        return imageResId;
    }
}
