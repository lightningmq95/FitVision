package com.example.fitvisionapp.network;
import java.util.List;

public class ApiResponseTemp {
    private String status;
    private int count;
    private List<ApiImageTemp> images;

    public String getStatus() {
        return status;
    }

    public int getCount() {
        return count;
    }

    public List<ApiImageTemp> getImages() {
        return images;
    }
}
