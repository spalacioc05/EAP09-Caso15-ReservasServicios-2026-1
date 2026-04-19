package com.eap09.reservas.security.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.eap09.reservas.security.config.SecurityProperties;
import com.eap09.reservas.security.domain.SecurityUserPrincipal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldGenerateTokenWithJtiAndExtractIt() {
        SecurityProperties props = new SecurityProperties();
        props.setJwtSecret("jwt-secret-non-base64-with-at-least-32-bytes-!!");
        props.setJwtExpirationSeconds(1800L);

        JwtService jwtService = new JwtService(props);
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
                1L,
                "user@example.com",
                "hash",
                "CLIENTE",
                true,
                true);

        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CLIENTE");
        claims.put("jti", jti);

        String token = jwtService.generateToken(claims, principal);

        assertNotNull(token);
        assertEquals("user@example.com", jwtService.extractUsername(token));
        assertEquals(jti, jwtService.extractTokenId(token));
    }
}
