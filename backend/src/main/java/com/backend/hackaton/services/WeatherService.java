package com.backend.hackaton.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.hackaton.dto.WeatherResponse;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    public WeatherResponse getWeather(Double latitude, Double longitude) {
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&hourly=temperature_2m,precipitation_probability,visibility,wind_speed_10m&current=precipitation,wind_speed_10m&forecast_days=1",
            latitude,
            longitude
        );
        return restTemplate.getForObject(url, WeatherResponse.class);
    }
}
