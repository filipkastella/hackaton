package com.backend.hackaton.dto;

import java.util.List;

import com.backend.hackaton.models.Position;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RouteRequest {

    private Position hostPos;
    private Position destination;
    private List<Double> route;
    
}
