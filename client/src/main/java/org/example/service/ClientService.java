package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final RestTemplate restTemplate;

    @Value("${ttp.url}")
    private String ttpUrl;

    @Value("${app-server.url}")
    private String serverUrl;

    @PostConstruct
    public void init() {
        pingTtp();
        pingServer();
    }

    private void pingTtp() {
        System.out.println("Klient pinguje TTP");
        try {
            String response = restTemplate.getForObject(
                    ttpUrl + "/api/ping",
                    String.class
            );
            System.out.println("TTP odpowiedzial: " + response);
        } catch (Exception e) {
            System.out.println("Nie udalo sie polaczyc z TTP: " + e.getMessage());
        }
    }

    private void pingServer() {
        System.out.println("Klient pinguje Server");
        try {
            String response = restTemplate.getForObject(
                    serverUrl + "/api/ping",
                    String.class
            );
            System.out.println("Server odpowiedzial: " + response);
        } catch (Exception e) {
            System.out.println("Nie udalo sie polaczyc z Server: " + e.getMessage());
        }
    }
}