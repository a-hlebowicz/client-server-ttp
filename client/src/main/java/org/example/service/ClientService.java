package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
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
    private PublicKey ttpPublicKey;

    @Value("${app-server.url}")
    private String serverUrl;

    private KeyPair clientKeyPair;
    private String clientId;
    private String certificate;
    private SecretKey sessionKey;

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
            this.ttpPublicKey = CryptoUtils.base64ToPublicKey((String) response.get("publicKey"));
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

    public AuthClientResponse startService() {
        try {
            requestServerService();
            return authInTtp();

        } catch (Exception e) {
            System.out.println("Blad podczas autoryzacji: " + e.getMessage());
            return null;
        }
    }

    private void requestServerService() {
        System.out.println("Client: Wysylam zadanie uslugi do Servera...");
        ServiceRequest request = new ServiceRequest(clientId, certificate);
        AuthServerResponse response = restTemplate.postForObject(
                serverUrl + "/api/request-service",
                request,
                AuthServerResponse.class
        );
        if (response == null || !"server_auth_ok".equals(response.getStatus())) {
            throw new RuntimeException("Server auth nieudane");
        }
        System.out.println("Client: " + response.getMessage());
    }

    private AuthClientResponse authInTtp() {
        System.out.println("Client: Uwierzytelniam sie w TTP...");
        String encryptedClientId = CryptoUtils.encryptWithPublicKey(ttpPublicKey, clientId);
        AuthClientRequest request = new AuthClientRequest(encryptedClientId, certificate);
        AuthClientResponse response = restTemplate.postForObject(
                ttpUrl + "/api/auth-client",
                request,
                AuthClientResponse.class
        );
        if (response == null) {
            throw new RuntimeException("Brak odpowiedzi z TTP");
        }
        System.out.println("Client: " + response.getMessage());
        String decryptedKeyBase64 = CryptoUtils.decryptWithPrivateKey(clientKeyPair.getPrivate(), response.getSessionKey());
        this.sessionKey = CryptoUtils.base64ToSessionKey(decryptedKeyBase64);
        System.out.println("Client: Otrzymalem klucz sesyjny");

        System.out.println("Client: " + response.getMessage());
        return response;
    }

    public String reverse(String text){
        if (sessionKey == null) {
            throw new RuntimeException("Brak klucza sesyjnego");
        }
        System.out.println("Client: Wysylam tekst do odwrocenia: " + text);
        String encryptedText = CryptoUtils.encryptWithSessionKey(sessionKey,text);
        ReverseRequest request = new ReverseRequest(encryptedText);
        ReverseResponse response = restTemplate.postForObject(
                serverUrl + "/api/reverse",
                request,
                ReverseResponse.class
        );

        String reversedText = CryptoUtils.decryptWithSessionKey(sessionKey, response.getEncryptedText());
        System.out.println("Client: Otrzymano odwrocony tekst: " + reversedText);
        return reversedText;
    }
    public void endSession() {
        if (sessionKey == null) {
            throw new RuntimeException("Brak aktywnej sesji");
        }
        System.out.println("Client: Konczenie sesji...");
        restTemplate.postForObject(
                serverUrl + "/api/end-session",
                null,
                Void.class
        );
        this.sessionKey = null;
        System.out.println("Client: Sesja zakonczona");
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