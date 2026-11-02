package com.hashcode.invoicely.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.CompanyUserDto;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.service.CompanyUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyUserServiceImpl implements CompanyUserService {

    private final CompanyUserRepository companyUserRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPreferences(UUID userId) {
        CompanyUserEntity link = resolveCompanyUser(userId);
        return parsePreferences(link.getPreferences());
    }

    @Override
    @Transactional
    public Map<String, Object> updatePreferences(UUID userId, Map<String, Object> preferencesPayload) {
        CompanyUserEntity link = resolveCompanyUser(userId);

        Map<String, Object> existing = new HashMap<>(parsePreferences(link.getPreferences()));
        if (preferencesPayload != null) {
            existing.putAll(preferencesPayload);
        }

        try {
            String json = objectMapper.writeValueAsString(existing);
            link.setPreferences(json);
            companyUserRepository.save(link);
        } catch (Exception e) {
            log.error("Failed to serialize preferences for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to save user preferences", e);
        }

        return existing;
    }

    @Override
    @Transactional
    public CompanyUserDto updateCompanyUser(UUID id, Map<String, Object> payload) {
        CompanyUserEntity link = companyUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CompanyUser not found: " + id));

        return CompanyUserDto.builder()
                .id(link.getId())
                .companyId(link.getCompanyId())
                .userId(link.getUserId())
                .accountId(link.getAccountId())
                .isOwner(link.isOwner())
                .isAdmin(link.isAdmin())
                .isLocked(link.isLocked())
                .permissions(String.valueOf(link.getPermissions()))
                .reactSettings(parsePreferences(link.getPreferences()))
                .createdAt(link.getCreatedAt() != null ? link.getCreatedAt().getEpochSecond() : null)
                .updatedAt(link.getUpdatedAt() != null ? link.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    private CompanyUserEntity resolveCompanyUser(UUID userId) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId != null) {
            Optional<CompanyUserEntity> linkOpt = companyUserRepository.findByCompanyIdAndUserId(companyId, userId);
            if (linkOpt.isPresent()) {
                return linkOpt.get();
            }
        }

        // Fallback: search by userId
        return companyUserRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CompanyUser not found for userId: " + userId));
    }

    private Map<String, Object> parsePreferences(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse preferences: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
