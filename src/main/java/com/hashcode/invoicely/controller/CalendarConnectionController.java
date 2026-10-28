package com.hashcode.invoicely.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class CalendarConnectionController {

    @GetMapping("/calendar_connection/events")
    public ResponseEntity<Map<String, Object>> getCalendarEvents(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        log.debug("Fetching calendar events from {} to {}", from, to);
        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "events", Collections.emptyList()
                )
        ));
    }

    @PostMapping("/one_time_token")
    public ResponseEntity<Map<String, Object>> generateOneTimeToken(@RequestBody(required = false) Map<String, Object> body) {
        String hash = UUID.randomUUID().toString().replace("-", "");
        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "hash", hash
                )
        ));
    }

    @PostMapping("/calendar_connection/{provider}/complete")
    public ResponseEntity<Map<String, Object>> completeCalendarConnection(
            @PathVariable String provider,
            @RequestBody(required = false) Map<String, Object> body) {
        log.info("Completed calendar connection for provider: {}", provider);
        return ResponseEntity.ok(Map.of("success", true, "provider", provider));
    }

    @DeleteMapping("/calendar_connection")
    public ResponseEntity<Map<String, Object>> disconnectCalendar() {
        log.info("Disconnected calendar integration");
        return ResponseEntity.ok(Map.of("success", true));
    }
}
