package org.example.data;

import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Component
public class TtpDataStore {

    private final Map<String, RegisteredEntity> registered = new HashMap<>();       //<id, dane>

    public void register(String entityId, String name, PublicKey publicKey, X509Certificate certificate) {
        registered.put(entityId, new RegisteredEntity(entityId, name, publicKey, certificate));
        System.out.println("Zarejestrowano: " + name + " ID: " + entityId);
    }

    public boolean exists(String entityId) {
        return registered.containsKey(entityId);
    }

    @Data
    @AllArgsConstructor
    public static class RegisteredEntity {
        private final String entityId;
        private final String name;
        private final PublicKey publicKey;
        private final X509Certificate certificate;
    }
}