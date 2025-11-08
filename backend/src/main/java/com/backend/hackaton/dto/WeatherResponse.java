package com.backend.hackaton.dto;

import lombok.Data;

@Data
public class WeatherResponse {
    private Double latitude;
    private Double longitude;
    private Double generationtime_ms;
    private Integer utc_offset_seconds;
    private String timezone;
    private String timezone_abbreviation;
    private Integer elevation;
    private CurrentUnitsDTO current_units;
    private CurrentDTO current;
    private HourlyUnitsDTO hourly_units;
    private HourlyDTO hourly;
}
