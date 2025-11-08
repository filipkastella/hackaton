package com.backend.hackaton.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.hackaton.services.WeatherService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("")
    public ResponseEntity<?> getWeather(Double latitude, Double longitude){
        if(weatherService.getWeather(latitude, longitude) != null){
            return ResponseEntity.ok(weatherService.getWeather(latitude, longitude));
        } else{
            return ResponseEntity.badRequest().body("Could not fetch weather data");
        }
    }
    
}
