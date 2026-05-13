package com.nailsalon.booking.controller;

import com.nailsalon.booking.dto.ApiResponse;
import com.nailsalon.booking.entity.PortfolioImage;
import com.nailsalon.booking.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ApiResponse<List<PortfolioImage>> getAll() {
        return ApiResponse.success(portfolioService.getAll());
    }

    @PostMapping
    public ApiResponse<Void> addImage(@RequestBody PortfolioImage image) {
        portfolioService.addImage(image);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteImage(@PathVariable Long id) {
        portfolioService.deleteImage(id);
        return ApiResponse.success(null);
    }
}
