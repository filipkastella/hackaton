package com.backend.hackaton.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);

        return filterFutureData(response);
    }

    private WeatherResponse filterFutureData(WeatherResponse response) {
        if(response == null || response.getHourly() == null){
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

        for (int i = 0; i < times.size(); i++){
            LocalDateTime timePoint = LocalDateTime.parse(times.get(i), formatter);

            if(!timePoint.isBefore(now)){
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
}
