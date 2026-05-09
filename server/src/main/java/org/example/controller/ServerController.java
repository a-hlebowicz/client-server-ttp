package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.service.ServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "service", "Server",
                "timestamp", LocalDateTime.now().toString(),
                "status", "ok"
        ));
    }

    @PostMapping("/request-service")
    public ResponseEntity<AuthServerResponse> requestService(@RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serverService.requestService(request));
    }

    @PostMapping("/notify-session-key")
    public ResponseEntity<Void> notifySessionKey(@RequestBody SessionKeyNotify notification) {
        serverService.receiveSessionKey(notification);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reverse")
    public ResponseEntity<ReverseResponse> requestReverse(@RequestBody ReverseRequest request){
        return ResponseEntity.ok(serverService.reverse(request));
    }

    @PostMapping("/end-session")
    public ResponseEntity<Void> endSession() {
        serverService.endSession();
        return ResponseEntity.ok().build();
    }
}