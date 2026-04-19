package com.eap09.reservas.security.application;

import com.eap09.reservas.security.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecurityProperties securityProperties;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(securityProperties.getJwtExpirationSeconds());
        String tokenId = extraClaims.get("jti") == null ? null : extraClaims.get("jti").toString();

        var builder = Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSignInKey());

        if (tokenId != null && !tokenId.isBlank()) {
            builder.id(tokenId);
        }

        return builder.compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, claims -> {
            String registeredTokenId = claims.getId();
            if (registeredTokenId != null && !registeredTokenId.isBlank()) {
                return registeredTokenId;
            }
            Object explicitJti = claims.get("jti");
            if (explicitJti != null) {
                return explicitJti.toString();
            }
            return null;
        });
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSignInKey() {
        String configuredSecret = securityProperties.getJwtSecret();
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(configuredSecret);
        } catch (RuntimeException ex) {
            keyBytes = configuredSecret.getBytes();
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
