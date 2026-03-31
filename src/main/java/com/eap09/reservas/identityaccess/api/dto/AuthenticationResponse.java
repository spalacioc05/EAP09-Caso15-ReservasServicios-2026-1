package com.eap09.reservas.identityaccess.api.dto;

public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String role
) {
}
