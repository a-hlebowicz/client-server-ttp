package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.dto.RegisterRequest;
import org.example.dto.RegisterResponse;
import java.security.*;
import java.util.Map;
import org.example.crypto.CryptoUtils;
@Service
@RequiredArgsConstructor
public class ServerService {

    private final RestTemplate restTemplate;

    @Value("${ttp.url}")
    private String ttpUrl;

    private KeyPair serverKeyPair;
    private String serverId;
    private String certificate;

    @PostConstruct       //SPRAWDZAMY CZY WIDZI TTP, PÓŹNIEJ WYRZUCIC
    public void init() {
        generateKeys();
        generateId();
        registerWithTtp();
    }
    private void generateKeys() {
        this.serverKeyPair = CryptoUtils.generateRSAKeyPair();
        System.out.println("Server: Wygenerowano pare kluczy RSA-4096");
    }

    private void generateId() {
        this.serverId = CryptoUtils.generateId("Server");
        System.out.println("Server: Wygenerowano ID: " + serverId);
    }

    private void registerWithTtp() {
        try {
            //pobieramy klucz publiczny ttp
            Map response = restTemplate.getForObject(ttpUrl + "/api/ttp-public-key", Map.class);
            PublicKey ttpPublicKey = CryptoUtils.base64ToPublicKey((String) response.get("publicKey"));
            System.out.println("Server: Pobrano klucz publiczny TTP");

            //szyfrujemy ID kluczem publicznym ttp
            String encryptedIdBase64 = CryptoUtils.encryptWithPublicKey(ttpPublicKey, serverId);

            //wysylamy rejestracje do ttp
            String publicKeyBase64 = CryptoUtils.publicKeyToBase64(serverKeyPair.getPublic());
            RegisterRequest request = new RegisterRequest(encryptedIdBase64, publicKeyBase64, "Server");
            RegisterResponse regResponse = restTemplate.postForObject(
                    ttpUrl + "/api/register", request, RegisterResponse.class);

            if (regResponse != null) {
                this.certificate = regResponse.getCertificate();
                System.out.println("Server: " + regResponse.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Server: Blad rejestracji w TTP: " + e.getMessage());
        }
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