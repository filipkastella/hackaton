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
        List<Integer> filteredPrecipitationProbabilities = new ArrayList<>();
        List<Long> filteredVisibility = new ArrayList<>();
        List<Double> filteredWindSpeed10m = new ArrayList<>();

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime timePoint = LocalDateTime.parse(times.get(i), formatter);

            if (!timePoint.isBefore(now)) {
                filteredTimes.add(times.get(i));
                filteredTemperatures.add(temperatures.get(i));
                filteredPrecipitationProbabilities.add(precipitationProbabilities.get(i));
                filteredVisibility.add(visibility.get(i));
                filteredWindSpeed10m.add(windSpeed10m.get(i));
            }
        }

        response.getHourly().setTime(filteredTimes);
        response.getHourly().setTemperature_2m(filteredTemperatures);
        response.getHourly().setPrecipitation_probability(filteredPrecipitationProbabilities);
        response.getHourly().setVisibility(filteredVisibility);
        response.getHourly().setWind_speed_10m(filteredWindSpeed10m);

        return response;

    }

    public RouteResponse getRouteWeather(RouteRequest route) {
        List<CoordinatesConditionDTO> responseList = new ArrayList<>();
        List<Double> routeCoordinates = new ArrayList<>();

        // Add host position
        routeCoordinates.add(route.getHostPos().getLatitude());
        routeCoordinates.add(route.getHostPos().getLongitude());

        // Add route waypoints
        for (Double coord : route.getRoute()) {
            routeCoordinates.add(coord);
        }

        // Add destination
        routeCoordinates.add(route.getDestination().getLatitude());
        routeCoordinates.add(route.getDestination().getLongitude());

        // Process each coordinate pair
        for (int i = 0; i < routeCoordinates.size(); i += 2) {
            Double latitude = routeCoordinates.get(i);
            Double longitude = routeCoordinates.get(i + 1);

            WeatherResponse weather = getWeather(latitude, longitude);
            String condition = analyzeWeatherCondition(weather);

            CoordinatesConditionDTO coordCondition = new CoordinatesConditionDTO();
            coordCondition.setLatitude(latitude);
            coordCondition.setLongitude(longitude);
            coordCondition.setCondition(condition);

            responseList.add(coordCondition);
        }

        RouteResponse response = new RouteResponse();
        response.setCoordinates(responseList);
        return response;
    }

    private String analyzeWeatherCondition(WeatherResponse weather) {
        if (weather == null || weather.getHourly() == null) {
            return "Unknown";
        }

        // Get the first hour's data (most immediate forecast)
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

        // Return combined condition or "Clear" if no adverse conditions
        if (conditions.isEmpty()) {
            return "Clear";
        }

        return String.join(", ", conditions);
    }
}
