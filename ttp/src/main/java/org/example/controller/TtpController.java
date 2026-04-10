package org.example.controller;
import lombok.RequiredArgsConstructor;
import org.example.dto.RegisterRequest;
import org.example.dto.RegisterResponse;
import org.example.service.TtpService;
import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Base64;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TtpController {

    private final TtpService ttpService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "service", "TTP",
                "timestamp", LocalDateTime.now().toString(),
                "status", "ok"
        ));
    }
    @GetMapping("/ttp-public-key")
    public ResponseEntity<Map<String, String>> getTtpPublicKey() {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(ttpService.getTtpPublicKey().getEncoded());
        return ResponseEntity.ok(Map.of("publicKey", publicKeyBase64));
    }
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = ttpService.register(request);
        return ResponseEntity.ok(response);
    }
}