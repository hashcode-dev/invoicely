package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.auth.CompanyUserDto;

import java.util.Map;
import java.util.UUID;

public interface CompanyUserService {
    Map<String, Object> getPreferences(UUID userId);
    Map<String, Object> updatePreferences(UUID userId, Map<String, Object> preferencesPayload);
    CompanyUserDto updateCompanyUser(UUID id, Map<String, Object> payload);
}
