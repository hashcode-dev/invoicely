package com.hashcode.invoicely.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String testSecret = "test-invoicely-enterprise-secret-key-must-be-at-least-256-bits-long-1234567890";
        jwtTokenProvider = new JwtTokenProvider(testSecret, 3600000L);
    }

    @Test
    @DisplayName("Should generate, parse, and validate JWT claims successfully")
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String email = "test@invoicely.com";

        String token = jwtTokenProvider.generateToken(userId, email, companyId, accountId);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo(email);
        assertThat(jwtTokenProvider.getCompanyId(token)).isEqualTo(companyId);
        assertThat(jwtTokenProvider.getAccountId(token)).isEqualTo(accountId);
    }

    @Test
    @DisplayName("Should reject malformed or invalid token")
    void shouldRejectInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
