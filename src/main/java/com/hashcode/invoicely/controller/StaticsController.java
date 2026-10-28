package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.statics.StaticsResponse;
import com.hashcode.invoicely.service.StaticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statics")
@RequiredArgsConstructor
public class StaticsController {

    private final StaticsService staticsService;

    @GetMapping
    public ResponseEntity<StaticsResponse> getStatics() {
        return ResponseEntity.ok(staticsService.getStatics());
    }
}
