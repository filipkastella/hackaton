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

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = (float) latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = (float) longitude;
    }
}
