package com.hashcode.invoicely.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${invoicely.jwt.secret:invoicely-enterprise-secret-key-must-be-at-least-256-bits-long}") String secret,
            @Value("${invoicely.jwt.expiration-ms:86400000}") long validityInMilliseconds) { // default 24h
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String generateToken(UUID userId, String email, UUID companyId, UUID accountId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("company_id", companyId != null ? companyId.toString() : null)
                .claim("account_id", accountId != null ? accountId.toString() : null)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public UUID getCompanyId(String token) {
        String companyId = getClaims(token).get("company_id", String.class);
        return companyId != null ? UUID.fromString(companyId) : null;
    }

    public UUID getAccountId(String token) {
        String accountId = getClaims(token).get("account_id", String.class);
        return accountId != null ? UUID.fromString(accountId) : null;
    }
}
