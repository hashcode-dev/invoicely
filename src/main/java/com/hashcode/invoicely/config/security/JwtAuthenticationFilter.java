package com.hashcode.invoicely.config.security;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.entity.relational.ApiTokenEntity;
import com.hashcode.invoicely.repository.relational.ApiTokenRepository;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_API_TOKEN = "X-Api-Token";
    public static final String HEADER_COMPANY_ID = "X-Company-ID";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ApiTokenRepository apiTokenRepository;
    private final CompanyUserRepository companyUserRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                if (jwtTokenProvider.validateToken(token)) {
                    UUID userId = jwtTokenProvider.getUserId(token);
                    String email = jwtTokenProvider.getEmail(token);
                    UUID companyId = jwtTokenProvider.getCompanyId(token);
                    UUID accountId = jwtTokenProvider.getAccountId(token);

                    // Allow override of active company only if verified member of that company
                    String companyHeader = request.getHeader(HEADER_COMPANY_ID);
                    if (StringUtils.hasText(companyHeader)) {
                        try {
                            UUID requestedCompanyId = UUID.fromString(companyHeader);
                            if (requestedCompanyId.equals(companyId) ||
                                    (userId != null && companyUserRepository.findByCompanyIdAndUserId(requestedCompanyId, userId).isPresent())) {
                                companyId = requestedCompanyId;
                            } else {
                                log.warn("User {} attempted unauthorized access to company {}", userId, requestedCompanyId);
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized company access");
                                return;
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    TenantContext.setUserId(userId);
                    TenantContext.setCompanyId(companyId);
                    TenantContext.setAccountId(accountId);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Check if it's an API Token stored in database
                    Optional<ApiTokenEntity> apiTokenOpt = apiTokenRepository.findByTokenHashAndIsDeletedFalse(token);
                    if (apiTokenOpt.isPresent()) {
                        ApiTokenEntity apiToken = apiTokenOpt.get();
                        TenantContext.setUserId(apiToken.getUserId());
                        TenantContext.setCompanyId(apiToken.getCompanyId());

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                apiToken.getName(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_TOKEN")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String apiToken = request.getHeader(HEADER_API_TOKEN);
        if (StringUtils.hasText(apiToken)) {
            return apiToken;
        }

        String bearer = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
