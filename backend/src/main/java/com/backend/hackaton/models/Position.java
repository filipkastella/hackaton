package com.backend.hackaton.models;

public class Position {

    private float longitude;
    private float latitude;

    public Position(float longitude, float latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Position() {
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }
}
