package com.backend.hackaton.dto;

import java.util.List;

public class RouteResponse {
    private List<CoordinatesConditionDTO> coordinates;

    public List<CoordinatesConditionDTO> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<CoordinatesConditionDTO> coordinates) {
        this.coordinates = coordinates;
    }
}