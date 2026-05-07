package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.dto.RegisterRequest;
import org.example.dto.RegisterResponse;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Map;
import org.example.crypto.CryptoUtils;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final RestTemplate restTemplate;

    @Value("${ttp.url}")
    private String ttpUrl;

    @Value("${app-server.url}")
    private String serverUrl;

    private KeyPair clientKeyPair;
    private String clientId;
    private String certificate;

    @PostConstruct
    public void init() {
        generateKeys();
        generateId();
        registerWithTtp();
    }
    private void generateKeys() {
        this.clientKeyPair = CryptoUtils.generateRSAKeyPair();
        System.out.println("Client: Wygenerowano pare kluczy RSA-4096");
    }

    private void generateId() {
        this.clientId = CryptoUtils.generateId("Client");
        System.out.println("Client: Wygenerowano ID: " + clientId);
    }

    private void registerWithTtp() {
        try {
            //pobieramy klucz publiczny ttp
            Map response = restTemplate.getForObject(ttpUrl + "/api/ttp-public-key", Map.class);
            PublicKey ttpPublicKey = CryptoUtils.base64ToPublicKey((String) response.get("publicKey"));
            System.out.println("Client: Pobrano klucz publiczny TTP");

            //szyfrujemy ID kluczem publicznym ttp
            String encryptedIdBase64 = CryptoUtils.encryptWithPublicKey(ttpPublicKey, clientId);

            //wysylamy rejestracje do ttp
            String publicKeyBase64 = CryptoUtils.publicKeyToBase64(clientKeyPair.getPublic());
            RegisterRequest request = new RegisterRequest(encryptedIdBase64, publicKeyBase64, "Client");
            RegisterResponse regResponse = restTemplate.postForObject(
                    ttpUrl + "/api/register", request, RegisterResponse.class);

            if (regResponse != null) {
                this.certificate = regResponse.getCertificate();
                System.out.println("Client: " + regResponse.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Client: Blad rejestracji w TTP: " + e.getMessage());
        }
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