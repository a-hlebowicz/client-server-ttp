package org.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "service", "Client",
                "timestamp", LocalDateTime.now().toString(),
                "status", "ok"
        ));
    }
}