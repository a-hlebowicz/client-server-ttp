package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "service", "Client",
                "timestamp", LocalDateTime.now().toString(),
                "status", "ok"
        ));
    }

    @PostMapping("/start-service")
    public ResponseEntity<?> startService() {
        return ResponseEntity.ok(clientService.startService());
    }

    @PostMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverse(@RequestBody Map<String, String> body) {
        String reversed = clientService.reverse(body.get("text"));
        return ResponseEntity.ok(Map.of("reversed", reversed));
    }

    @PostMapping("/end-session")
    public ResponseEntity<Map<String, String>> endSession() {
        clientService.endSession();
        return ResponseEntity.ok(Map.of("status", "session_ended"));
    }
}