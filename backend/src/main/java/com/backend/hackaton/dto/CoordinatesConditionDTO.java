package com.backend.hackaton.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoordinatesConditionDTO {
    private Double latitude;
    private Double longitude;
    private String condition;
}
