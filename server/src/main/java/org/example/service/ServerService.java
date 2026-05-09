package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.dto.*;
import java.security.*;
import java.util.Map;
import org.example.crypto.CryptoUtils;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final RestTemplate restTemplate;

    @Value("${ttp.url}")
    private String ttpUrl;

    private KeyPair serverKeyPair;
    private String serverId;
    private String certificate;
    private SecretKey sessionKey;

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

    public AuthServerResponse requestService(ServiceRequest request) {
        System.out.println("Server: Client " + request.getClientId() + "prosi o usluge");

        AuthServerRequest authRequest = new AuthServerRequest(
                serverId,
                certificate,
                request.getClientId(),
                request.getClientCertificate()
        );

        System.out.println("Server: Wysylam request uwierzytelniania do TTP...");
        AuthServerResponse response = restTemplate.postForObject(
                ttpUrl + "/api/auth-server",
                authRequest,
                AuthServerResponse.class
        );

        if (response != null) {
            System.out.println("Server: TTP odpowiedzial: " + response.getMessage());
        }

        return response;
    }

    public void receiveSessionKey(SessionKeyNotify notification) {
        String decryptedKeyBase64 = CryptoUtils.decryptWithPrivateKey(serverKeyPair.getPrivate(), notification.getSessionKey());
        this.sessionKey = CryptoUtils.base64ToSessionKey(decryptedKeyBase64);
        System.out.println("Server: Otrzymalem klucz sesyjny od TTP");
    }

    public ReverseResponse reverse(ReverseRequest request){
        if (sessionKey == null) {
            throw new RuntimeException("Brak klucza sesyjnego");
        }
        String encryptedText = request.getEncryptedText();
        String text = CryptoUtils.decryptWithSessionKey(sessionKey,encryptedText);
        System.out.println("Server: Otrzymano tekst: " + text);

        String reversedText = reverseText(text);
        System.out.println("Server: Odwrocony tekst: " + reversedText);
        String encryptedReversedText = CryptoUtils.encryptWithSessionKey(sessionKey,reversedText);

        return new ReverseResponse(encryptedReversedText);
    }

    private String reverseText(String text){
        return new StringBuilder(text).reverse().toString();
    }

    public void endSession() {
        this.sessionKey = null;
        System.out.println("Server: Sesja zakonczona");
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