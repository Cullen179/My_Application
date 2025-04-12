package com.example.myapplication.data.model;

public class DeepfakeResponse {
    private float fakePercentage;
    private float realPercentage;

    public DeepfakeResponse(float fakePercentage, float realPercentage) {
        this.fakePercentage = fakePercentage;
        this.realPercentage = realPercentage;
    }

    public float getFakePercentage() {
        return fakePercentage;
    }

    public float getRealPercentage() {
        return realPercentage;
    }

    public boolean getPrediction() {
        if (fakePercentage > realPercentage) {
            return true;
        } else {
            return false;
        }
    }
}