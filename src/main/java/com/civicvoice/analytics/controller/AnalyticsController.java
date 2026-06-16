package com.civicvoice.analytics.controller;

import com.civicvoice.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AUTHORITY', 'NGO', 'ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStats() {
        return analyticsService.getDashboardStats();
    }

    @GetMapping("/sla")
    public List<Map<String, Object>> getSlaCompliance() {
        return analyticsService.getSlaCompliance();
    }

    @GetMapping("/trends")
    public List<Map<String, Object>> getTrends() {
        return analyticsService.getTrends();
    }

    @GetMapping("/department-kpis")
    public List<Map<String, Object>> getDepartmentKpis() {
        return analyticsService.getDepartmentKpis();
    }

    @GetMapping("/poll-engagement")
    public Map<String, Object> getPollEngagement() {
        return analyticsService.getPollEngagement();
    }

    @GetMapping("/category-breakdown")
    public List<Map<String, Object>> getCategoryBreakdown() {
        return analyticsService.getCategoryBreakdown();
    }
}
