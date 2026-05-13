package com.nailsalon.booking.controller;

import com.nailsalon.booking.dto.ApiResponse;
import com.nailsalon.booking.service.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class StoreSettingsController {
    private final StoreSettingsService service;

    @GetMapping
    public ApiResponse<Map<String, String>> getSettings() {
        return ApiResponse.success(service.getAllSettings());
    }

    @PostMapping
    public ApiResponse<Void> updateSettings(@RequestBody Map<String, String> settings) {
        settings.forEach(service::updateSetting);
        return ApiResponse.success(null);
    }
}
