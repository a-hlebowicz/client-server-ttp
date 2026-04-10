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
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096);
            this.serverKeyPair = generator.generateKeyPair();
            System.out.println("Server: Wygenerowano pare kluczy RSA-4096");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Blad generowania kluczy RSA", e);
        }
    }

    private void generateId() {
        try {
            String raw = "Server_" + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            this.serverId = Base64.getEncoder().encodeToString(hash);
            System.out.println("Server: Wygenerowano ID: " + serverId);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Blad generowania SHA-256", e);
        }
    }

    private void registerWithTtp() {
        try {
            //pobieramy klucz publiczny ttp
            Map response = restTemplate.getForObject(ttpUrl + "/api/ttp-public-key", Map.class);
            byte[] ttpPublicKeyBytes = Base64.getDecoder().decode((String) response.get("publicKey"));
            PublicKey ttpPublicKey = KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(ttpPublicKeyBytes));
            System.out.println("Server: Pobrano klucz publiczny TTP");

            //szyfrujemy ID kluczem publicznym ttp
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, ttpPublicKey);
            byte[] encryptedId = cipher.doFinal(serverId.getBytes(StandardCharsets.UTF_8));
            String encryptedIdBase64 = Base64.getEncoder().encodeToString(encryptedId);

            //wysylamy rejestracje do ttp
            String publicKeyBase64 = Base64.getEncoder().encodeToString(serverKeyPair.getPublic().getEncoded());
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