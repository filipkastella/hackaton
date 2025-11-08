package com.backend.hackaton.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.hackaton.dto.CoordinatesConditionDTO;
import com.backend.hackaton.dto.RouteRequest;
import com.backend.hackaton.dto.RouteResponse;
import com.backend.hackaton.dto.WeatherResponse;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    public WeatherResponse getWeather(Double latitude, Double longitude) {
        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&hourly=temperature_2m,precipitation_probability,visibility,wind_speed_10m&current=precipitation,wind_speed_10m&forecast_days=1",
                latitude,
                longitude);
        WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);

        return filterFutureData(response);
    }

    private WeatherResponse filterFutureData(WeatherResponse response) {
        if (response == null || response.getHourly() == null) {
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<String> times = response.getHourly().getTime();
        List<Double> temperatures = response.getHourly().getTemperature_2m();
        List<Integer> precipitationProbabilities = response.getHourly().getPrecipitation_probability();
        List<Long> visibility = response.getHourly().getVisibility();
        List<Double> windSpeed10m = response.getHourly().getWind_speed_10m();

        List<String> filteredTimes = new ArrayList<>();
        List<Double> filteredTemperatures = new ArrayList<>();
        List<Integer> filteredPrecipitation = new ArrayList<>();
        List<Long> filteredVisibility = new ArrayList<>();
        List<Double> filteredWindSpeed = new ArrayList<>();

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime time = LocalDateTime.parse(times.get(i), formatter);
            if (time.isAfter(now)) {
                filteredTimes.add(times.get(i));
                filteredTemperatures.add(temperatures.get(i));
                filteredPrecipitation.add(precipitationProbabilities.get(i));
                filteredVisibility.add(visibility.get(i));
                filteredWindSpeed.add(windSpeed10m.get(i));
            }
        }

        response.getHourly().setTime(filteredTimes);
        response.getHourly().setTemperature_2m(filteredTemperatures);
        response.getHourly().setPrecipitation_probability(filteredPrecipitation);
        response.getHourly().setVisibility(filteredVisibility);
        response.getHourly().setWind_speed_10m(filteredWindSpeed);

        return response;
    }

    public RouteResponse getRouteWeather(RouteRequest route) {
        List<CoordinatesConditionDTO> responseList = new ArrayList<>();
        
        try {
            // Sample every Nth coordinate to reduce API calls (e.g., every 10th point)
            int sampleRate = 10;
            List<Double> sampledCoords = new ArrayList<>();

            // Add host position
            sampledCoords.add(route.getHostPos().getLatitude());
            sampledCoords.add(route.getHostPos().getLongitude());

            // Sample route waypoints
            List<Double> routeCoords = route.getRoute();
            for (int i = 0; i < routeCoords.size(); i += 2 * sampleRate) {
                if (i < routeCoords.size() - 1) {
                    sampledCoords.add(routeCoords.get(i));
                    sampledCoords.add(routeCoords.get(i + 1));
                }
            }

            // Add destination
            sampledCoords.add(route.getDestination().getLatitude());
            sampledCoords.add(route.getDestination().getLongitude());

            System.out.println("Processing " + (sampledCoords.size() / 2) + " coordinate pairs (sampled from " + (routeCoords.size() / 2) + " waypoints)");

            // Process each sampled coordinate pair
            for (int i = 0; i < sampledCoords.size(); i += 2) {
                try {
                    Double latitude = sampledCoords.get(i);
                    Double longitude = sampledCoords.get(i + 1);

                    WeatherResponse weather = getWeather(latitude, longitude);
                    String condition = analyzeWeatherCondition(weather);

                    CoordinatesConditionDTO coordCondition = new CoordinatesConditionDTO();
                    coordCondition.setLatitude(latitude);
                    coordCondition.setLongitude(longitude);
                    coordCondition.setCondition(condition);

                    responseList.add(coordCondition);
                    
                    // Small delay to avoid overwhelming the API
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Error processing coordinate pair: " + e.getMessage());
                }
            }

            RouteResponse response = new RouteResponse();
            response.setCoordinates(responseList);
            return response;
            
        } catch (Exception e) {
            System.err.println("Error in getRouteWeather: " + e.getMessage());
            e.printStackTrace();
            
            RouteResponse errorResponse = new RouteResponse();
            errorResponse.setCoordinates(new ArrayList<>());
            return errorResponse;
        }
    }

    private String analyzeWeatherCondition(WeatherResponse weather) {
        if (weather == null || weather.getHourly() == null) {
            return "Unknown";
        }

        List<Integer> precipitation = weather.getHourly().getPrecipitation_probability();
        List<Long> visibility = weather.getHourly().getVisibility();
        List<Double> windSpeed = weather.getHourly().getWind_speed_10m();

        if (precipitation.isEmpty() || visibility.isEmpty() || windSpeed.isEmpty()) {
            return "Unknown";
        }

        int precipProb = precipitation.get(0);
        long visibilityMeters = visibility.get(0);
        double wind = windSpeed.get(0);

        List<String> conditions = new ArrayList<>();

        // Analyze precipitation
        if (precipProb > 80) {
            conditions.add("Heavy Storm");
        } else if (precipProb > 55) {
            conditions.add("Storm");
        } else if (precipProb > 30) {
            conditions.add("Raining");
        } else if (precipProb > 10) {
            conditions.add("Light Rain");
        }

        // Analyze visibility
        if (visibilityMeters < 1000) {
            conditions.add("Dense Fog");
        } else if (visibilityMeters < 5000) {
            conditions.add("Foggy");
        } else if (visibilityMeters < 10000) {
            conditions.add("Reduced Visibility");
        }

        // Analyze wind
        if (wind > 60) {
            conditions.add("Severe Winds");
        } else if (wind > 40) {
            conditions.add("Strong Winds");
        } else if (wind > 25) {
            conditions.add("Windy");
        }

        if (conditions.isEmpty()) {
            return "Clear";
        }

        return String.join(", ", conditions);
    }
}