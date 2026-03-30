package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final RestTemplate restTemplate;

    @Value("${ttp.url}")
    private String ttpUrl;

    @PostConstruct       //SPRAWDZAMY CZY WIDZI TTP, PÓŹNIEJ WYRZUCIC
    public void init() {
        pingTtp();
    }

    private void pingTtp() {
        System.out.println("Server pinguje TTP");
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
}