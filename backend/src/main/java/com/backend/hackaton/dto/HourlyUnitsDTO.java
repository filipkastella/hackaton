package com.backend.hackaton.dto;

import lombok.Data;

@Data
public class HourlyUnitsDTO {
    private String time;
    private String temperature_2m;
    private String precipitation_probability;
    private String visibility;
    private String wind_speed_10m;
}
