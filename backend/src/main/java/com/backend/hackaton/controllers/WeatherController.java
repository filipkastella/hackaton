package com.backend.hackaton.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.hackaton.dto.RouteRequest;
import com.backend.hackaton.services.WeatherService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for weather-related operations.
 * 
 * <p>This controller provides endpoints for fetching weather data for single
 * coordinates and analyzing weather conditions along a route.</p>
 * 
 * @author Hackaton Team
 * @version 1.0
 * @since 2025-11-08
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * Fetches weather data for a specific geographic coordinate.
     * 
     * @param latitude the latitude of the location
     * @param longitude the longitude of the location
     * @return ResponseEntity with WeatherResponse containing forecast data or error message
     */
    @GetMapping("")
    public ResponseEntity<?> getWeather(Double latitude, Double longitude){
        if(weatherService.getWeather(latitude, longitude) != null){
            return ResponseEntity.ok(weatherService.getWeather(latitude, longitude));
        } else{
            return ResponseEntity.badRequest().body("Could not fetch weather data");
        }
    }
    
    /**
     * Analyzes weather conditions along a route.
     * 
     * <p>Samples coordinates along the route (host position, waypoints, and destination)
     * and returns weather conditions for each sampled point. To avoid API rate limits,
     * only every 10th waypoint is sampled.</p>
     * 
     * @param route the route request containing start position, waypoints, and destination
     * @return ResponseEntity with RouteResponse containing weather conditions for sampled coordinates
     */
    @PostMapping("/route")
    public ResponseEntity<?> getRouteCoordinates(@RequestBody RouteRequest route){
        if(weatherService.getRouteWeather(route) != null){
            return ResponseEntity.ok(weatherService.getRouteWeather(route));
        } else{
            return ResponseEntity.badRequest().body("Could not fetch weather data for the route");
        }
    }

    
}
