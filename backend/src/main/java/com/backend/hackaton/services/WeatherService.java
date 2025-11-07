package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    @Value("${copernicus.api.url}")
    private String apiUrl;

    @Value("${copernicus.api.key}")
    private String apiKey;

    
}
