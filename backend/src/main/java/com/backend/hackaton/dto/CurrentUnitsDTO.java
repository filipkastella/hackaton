package com.backend.hackaton.dto;

import lombok.Data;

@Data
public class CurrentUnitsDTO {
    private String time;
    private String interval;
    private String precipitation;
    private String wind_speed_10m;
}
