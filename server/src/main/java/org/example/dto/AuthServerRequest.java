package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthServerRequest {
    private String serverId;
    private String serverCertificate;
    private String clientId;
    private String clientCertificate;
}