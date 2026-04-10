package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.data.TtpDataStore;
import org.example.dto.RegisterRequest;
import org.example.dto.RegisterResponse;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.*;

import javax.crypto.Cipher;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TtpService {

    private final TtpDataStore dataStore;

    private KeyPair ttpKeyPair;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096);
            this.ttpKeyPair = generator.generateKeyPair();
            System.out.println("TTP: Wygenerowano pare kluczy RSA-4096");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Blad generowania kluczy RSA", e);
        }
    }

    public PublicKey getTtpPublicKey() {
        return ttpKeyPair.getPublic();
    }

    public RegisterResponse register(RegisterRequest request) {
        try {
            //dekodujemy klucz publiczny
            byte[] publicKeyBytes = Base64.getDecoder().decode(request.getPublicKey());
            PublicKey senderPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            //deszyfrujemy id kluczem prywatnym ttp
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, ttpKeyPair.getPrivate());
            byte[] decryptedIdBytes = cipher.doFinal(Base64.getDecoder().decode(request.getEncryptedId()));
            String entityId = new String(decryptedIdBytes);

            //sprawdzamy czy juz istnieje
            if (dataStore.exists(entityId)) {
                return new RegisterResponse("already_registered", request.getName() + " juz zarejestrowany", null);
            }

            //tworzymy certyfikat X.509
            X509Certificate certificate = createCertificate(
                    request.getName(), senderPublicKey,
                    ttpKeyPair.getPrivate(), ttpKeyPair.getPublic()
            );

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

    private X509Certificate createCertificate(String subjectName, PublicKey subjectPublicKey, PrivateKey ttpPrivateKey, PublicKey ttpPublicKey)
            throws Exception {

        X500Principal issuer = new X500Principal("CN=TTP");
        X500Principal subject = new X500Principal("CN=" + subjectName);

        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 100L * 24 * 60 * 60 * 1000);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                subjectPublicKey
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(ttpPrivateKey);

        return new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
    }
}