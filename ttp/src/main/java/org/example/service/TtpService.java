package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.crypto.CryptoUtils;
import org.example.data.TtpDataStore;
import org.example.dto.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.security.*;

import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.example.crypto.CryptoUtils.base64ToCertificate;
import static org.example.crypto.CryptoUtils.createCertificate;

import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class TtpService {

    private final TtpDataStore dataStore;
    private final RestTemplate restTemplate;
    @Value("${app-server.url}")
    private String serverUrl;

    private KeyPair ttpKeyPair;

    @PostConstruct
    public void init() {
        this.ttpKeyPair = CryptoUtils.generateRSAKeyPair();
        System.out.println("TTP: Wygenerowano pare kluczy RSA-4096");
    }

    public PublicKey getTtpPublicKey() {
        return ttpKeyPair.getPublic();
    }

    public RegisterResponse register(RegisterRequest request) {
        try {
            //dekodujemy klucz publiczny
            PublicKey senderPublicKey = CryptoUtils.base64ToPublicKey(request.getPublicKey());

            //deszyfrujemy id kluczem prywatnym ttp
            String entityId = CryptoUtils.decryptWithPrivateKey(ttpKeyPair.getPrivate(), request.getEncryptedId());

            //sprawdzamy czy juz istnieje
            if (dataStore.exists(entityId)) {
                return new RegisterResponse("already_registered", request.getName() + " juz zarejestrowany", null);
            }

            //tworzymy certyfikat X.509
            X509Certificate certificate = createCertificate(request.getName(), senderPublicKey, ttpKeyPair.getPrivate());

            //zapisujemy
            dataStore.register(entityId, request.getName(), senderPublicKey, certificate);

            //zwracamy certyfilat i klucz publiczny
            String cert64 = Base64.getEncoder().encodeToString(certificate.getEncoded());

            return new RegisterResponse("registered", request.getName() + " zarejestrowany w TTP",cert64);

        } catch (Exception e) {
            System.out.println("Blad rejestracji: " + e.getMessage());
            return new RegisterResponse("error", "Blad rejestracji: " + e.getMessage(), null);
        }
    }

    public AuthServerResponse authServer (AuthServerRequest request){
        try {
            //czy istnieje
            if (!dataStore.exists(request.getServerId())) {
                return new AuthServerResponse("error", "Server niezarejestrowany");
            }
            if (!dataStore.exists(request.getClientId())) {
                return new AuthServerResponse("error", "Client niezarejestrowany");
            }

            //sprawdzamy certyfikat
            X509Certificate serverCert = base64ToCertificate(request.getServerCertificate());
            serverCert.verify(ttpKeyPair.getPublic());

            System.out.println("TTP: Server " + request.getServerId() + "uwierzytelniony");
            return new AuthServerResponse("server_auth_ok", "Server uwierzytelniony");

        } catch (Exception e){
            System.out.println("TTP: Blad uwierzytelniania Servera: " + e.getMessage());
            return new AuthServerResponse("error", "Blad weryfikacji x509: " + e.getMessage());
        }
    }

    public AuthClientResponse authClient(AuthClientRequest request){
        try{
            String clientId = validateAndGetClientId(request);
            SecretKey sessionKey = CryptoUtils.generateSessionKey();

            PublicKey clientPublicKey = dataStore.find(clientId).getPublicKey();
            String sessionKeyBase64 = CryptoUtils.sessionKeyToBase64(sessionKey);
            sendSessionKeyToServer(sessionKey);
            System.out.println("TTP: Klucz sesyjny wyslany do Clienta");
            return new AuthClientResponse(
                    "client_auth_ok",
                    "Client uwierzytelniony",
                    CryptoUtils.encryptWithPublicKey(clientPublicKey, sessionKeyBase64)
            );

        } catch(Exception e){
            System.out.println("TTP: Blad uwierzytelniania Clienta: " + e.getMessage());
            return new AuthClientResponse("error", e.getMessage(), null);
        }
    }
    private String validateAndGetClientId(AuthClientRequest request) throws Exception {
        String clientId = CryptoUtils.decryptWithPrivateKey(
                ttpKeyPair.getPrivate(), request.getEncryptedClientId());

        if (!dataStore.exists(clientId)) {
            throw new RuntimeException("Client niezarejestrowany");
        }

        X509Certificate clientCert = base64ToCertificate(request.getClientCertificate());
        clientCert.verify(ttpKeyPair.getPublic());

        System.out.println("TTP: Client " + clientId + "zweryfikowany");
        return clientId;
    }
    private void sendSessionKeyToServer(SecretKey sessionKey) {
        TtpDataStore.RegisteredEntity server = dataStore.findByName("Server");
        if (server == null) {
            throw new RuntimeException("Server niezarejestrowany");
        }

        String sessionKeyBase64 = CryptoUtils.sessionKeyToBase64(sessionKey);
        String encryptedForServer = CryptoUtils.encryptWithPublicKey(server.getPublicKey(), sessionKeyBase64);

        SessionKeyNotify notification = new SessionKeyNotify("client_auth_ok", encryptedForServer);
        restTemplate.postForObject(serverUrl + "/api/notify-session-key", notification, Void.class);

        System.out.println("TTP: Klucz sesyjny wyslany do Servera");
    }
}