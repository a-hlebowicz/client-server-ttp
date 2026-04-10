package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String encryptedId;     // id zaszyfrowane kluczem publixznym
    private String publicKey;       // klucz publiczny nadawcy
    private String name;            // wysyłajacy czyli server lub client
}