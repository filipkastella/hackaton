package com.backend.hackaton.dto;

import java.util.List;

import lombok.Data;

@Data
public class HourlyDTO {
    private List<String> time;
    private List<Double> temperature_2m;
    private List<Integer> precipitation_probability;
    private List<Long> visibility;
    private List<Double> wind_speed_10m;
}
