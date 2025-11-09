package com.backend.hackaton.models;

import lombok.ToString;

/**
 * Represents a geographic coordinate position.
 * 
 * <p>Used for tracking locations of members and destinations in group trips.</p>
 * 
 * @author Hackaton Team
 * @version 1.0
 * @since 2025-11-08
 */
@ToString
public class Position {

    private Double longitude;
    private Double latitude;

    /**
     * Constructs a Position with specified coordinates.
     * 
     * @param longitude the longitude value
     * @param latitude the latitude value
     */
    public Position(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Default constructor for deserialization.
     */
    public Position() {
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
