package com.ayushi.expensetracker.controller;

import com.ayushi.expensetracker.service.ForecastClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastClientService forecastClientService;

    public ForecastController(ForecastClientService forecastClientService) {
        this.forecastClientService = forecastClientService;
    }

    @GetMapping("/overall")
    public ForecastClientService.ForecastResponse overallForecast() {
        // TODO: replace with real monthly totals pulled from ExpenseRepository
        List<ForecastClientService.MonthlyTotal> history = List.of();
        return forecastClientService.getOverallForecast(history);
    }

    @GetMapping("/category/{category}")
    public ForecastClientService.ForecastResponse categoryForecast(@PathVariable String category) {
        // TODO: replace with real monthly totals pulled from ExpenseRepository
        List<ForecastClientService.MonthlyTotal> history = List.of();
        return forecastClientService.getCategoryForecast(category, history);
    }
}
