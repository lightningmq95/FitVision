package com.example.fitvisionapp.models;

public class ComboImage {
    public String imageName;
    public String imageData;  // Base64 string or URL
    public String category;

    // Constructor
    public ComboImage(String imageName, String imageData, String category) {
        this.imageName = imageName;
        this.imageData = imageData;
        this.category = category;
    }

    // Getters (optional, useful for RecyclerView binding)
    public String getImageName() {
        return imageName;
    }

    public String getImageData() {
        return imageData;
    }

    public String getCategory() {
        return category;
    }
}
