package com.backend.hackaton.dto;

import lombok.Data;

@Data
public class CurrentDTO {
    private String time;
    private Integer interval;
    private Double precipitation;
    private Double wind_speed_10m;
}
