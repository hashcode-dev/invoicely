package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.navigation.NavigationConfigDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NavigationConfigService {
    Map<String, NavigationConfigDto> getResolvedNavigationConfig(UUID companyId, UUID userId);
    List<NavigationConfigDto> getCompanyNavigationRules(UUID companyId, String role);
    List<NavigationConfigDto> saveCompanyNavigationRules(UUID companyId, String role, List<NavigationConfigDto> items);
}
