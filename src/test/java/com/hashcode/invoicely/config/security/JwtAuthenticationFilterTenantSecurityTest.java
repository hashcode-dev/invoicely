package com.hashcode.invoicely.config.security;

import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.repository.relational.ApiTokenRepository;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTenantSecurityTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ApiTokenRepository apiTokenRepository;

    @Mock
    private CompanyUserRepository companyUserRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID userId;
    private UUID authorizedCompanyId;
    private UUID unauthorizedCompanyId;
    private String validToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authorizedCompanyId = UUID.randomUUID();
        unauthorizedCompanyId = UUID.randomUUID();
        validToken = "mock.valid.jwt.token";

        when(request.getHeader(JwtAuthenticationFilter.HEADER_API_TOKEN)).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserId(validToken)).thenReturn(userId);
        when(jwtTokenProvider.getEmail(validToken)).thenReturn("user@invoicely.com");
        when(jwtTokenProvider.getCompanyId(validToken)).thenReturn(authorizedCompanyId);
        when(jwtTokenProvider.getAccountId(validToken)).thenReturn(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should reject request with 403 Forbidden when user attempts unauthorized X-Company-ID override")
    void shouldRejectUnauthorizedCompanyHeaderOverride() throws Exception {
        when(request.getHeader(JwtAuthenticationFilter.HEADER_COMPANY_ID))
                .thenReturn(unauthorizedCompanyId.toString());
        when(companyUserRepository.findByCompanyIdAndUserId(unauthorizedCompanyId, userId))
                .thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized company access");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should permit request when user has authorized membership in X-Company-ID target company")
    void shouldPermitAuthorizedCompanyHeaderOverride() throws Exception {
        when(request.getHeader(JwtAuthenticationFilter.HEADER_COMPANY_ID))
                .thenReturn(unauthorizedCompanyId.toString());
        when(companyUserRepository.findByCompanyIdAndUserId(unauthorizedCompanyId, userId))
                .thenReturn(Optional.of(CompanyUserEntity.builder().build()));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }
}
