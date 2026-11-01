package com.ayushi.expensetracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Talks to the Python (Flask + scikit-learn) forecasting microservice.
 * Keeps the cross-service HTTP boundary in one place.
 */
@Service
public class ForecastClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ml.service.base-url}")
    private String mlServiceBaseUrl;

    public record MonthlyTotal(String month, double amount) {}

    public record ForecastResponse(double predictedAmount, String confidenceNote) {}

    public ForecastResponse getOverallForecast(List<MonthlyTotal> history) {
        Map<String, Object> body = Map.of("history", history);
        return restTemplate.postForObject(
                mlServiceBaseUrl + "/forecast/overall", body, ForecastResponse.class);
    }

    public ForecastResponse getCategoryForecast(String category, List<MonthlyTotal> history) {
        Map<String, Object> body = Map.of("history", history);
        return restTemplate.postForObject(
                mlServiceBaseUrl + "/forecast/category/" + category, body, ForecastResponse.class);
    }
}
