package com.backend.hackaton.dto;

import java.util.List;

import com.backend.hackaton.models.Position;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteRequest {

    private Position hostPos;
    private Position destination;
    private List<Double> route;
    
}
