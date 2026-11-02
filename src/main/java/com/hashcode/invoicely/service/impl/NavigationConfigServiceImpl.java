package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.navigation.NavigationConfigDto;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.TenantNavigationConfigEntity;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.repository.relational.TenantNavigationConfigRepository;
import com.hashcode.invoicely.service.NavigationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationConfigServiceImpl implements NavigationConfigService {

    private final TenantNavigationConfigRepository navigationConfigRepository;
    private final CompanyUserRepository companyUserRepository;

    private static final List<String> STANDARD_NAV_ITEMS = List.of(
            "dashboard", "clients", "products", "invoices", "recurring_invoices",
            "payments", "quotes", "credits", "projects", "tasks",
            "vendors", "purchase_orders", "expenses", "recurring_expenses",
            "transactions", "reports", "employees", "settings"
    );

    @Override
    @Transactional(readOnly = true)
    public Map<String, NavigationConfigDto> getResolvedNavigationConfig(UUID companyId, UUID userId) {
        if (companyId == null) {
            companyId = TenantContext.getCompanyId();
        }
        if (userId == null) {
            userId = TenantContext.getUserId();
        }

        boolean isEmployee = true;
        if (companyId != null && userId != null) {
            Optional<CompanyUserEntity> userLink = companyUserRepository.findByCompanyIdAndUserId(companyId, userId);
            if (userLink.isPresent()) {
                isEmployee = !userLink.get().isOwner() && !userLink.get().isAdmin();
            }
        }

        String targetRole = isEmployee ? "EMPLOYEE" : "ADMIN";

        Map<String, NavigationConfigDto> result = new LinkedHashMap<>();

        // 1. Seed with platform defaults
        int order = 1;
        for (String key : STANDARD_NAV_ITEMS) {
            boolean visible = true;
            if (isEmployee && (key.equals("employees") || key.equals("reports") || key.equals("settings"))) {
                visible = false;
            }
            result.put(key, NavigationConfigDto.builder()
                    .itemKey(key)
                    .isVisible(visible)
                    .displayOrder(order++)
                    .role(targetRole)
                    .build());
        }

        // 2. Overlay tenant-configured rules if companyId present
        if (companyId != null) {
            List<TenantNavigationConfigEntity> savedRules =
                    navigationConfigRepository.findByCompanyIdAndRoleOrderByDisplayOrderAsc(companyId, targetRole);

            if (savedRules.isEmpty() && isEmployee) {
                // Fallback to DEFAULT company rules
                savedRules = navigationConfigRepository.findByCompanyIdAndRoleOrderByDisplayOrderAsc(companyId, "DEFAULT");
            }

            for (TenantNavigationConfigEntity rule : savedRules) {
                result.put(rule.getItemKey(), NavigationConfigDto.builder()
                        .itemKey(rule.getItemKey())
                        .isVisible(rule.isVisible())
                        .displayOrder(rule.getDisplayOrder())
                        .customLabel(rule.getCustomLabel())
                        .isLocked(rule.isLocked())
                        .role(rule.getRole())
                        .build());
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NavigationConfigDto> getCompanyNavigationRules(UUID companyId, String role) {
        if (companyId == null) {
            companyId = TenantContext.getCompanyId();
        }
        String normalizedRole = (role != null && !role.isBlank()) ? role.toUpperCase() : "DEFAULT";

        List<TenantNavigationConfigEntity> entities =
                navigationConfigRepository.findByCompanyIdAndRoleOrderByDisplayOrderAsc(companyId, normalizedRole);

        if (entities.isEmpty()) {
            return new ArrayList<>(getResolvedNavigationConfig(companyId, null).values());
        }

        return entities.stream()
                .map(e -> NavigationConfigDto.builder()
                        .itemKey(e.getItemKey())
                        .isVisible(e.isVisible())
                        .displayOrder(e.getDisplayOrder())
                        .customLabel(e.getCustomLabel())
                        .isLocked(e.isLocked())
                        .role(e.getRole())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public List<NavigationConfigDto> saveCompanyNavigationRules(UUID companyId, String role, List<NavigationConfigDto> items) {
        if (companyId == null) {
            companyId = TenantContext.getCompanyId();
        }
        String normalizedRole = (role != null && !role.isBlank()) ? role.toUpperCase() : "DEFAULT";

        List<NavigationConfigDto> savedResults = new ArrayList<>();

        for (NavigationConfigDto item : items) {
            Optional<TenantNavigationConfigEntity> existingOpt =
                    navigationConfigRepository.findByCompanyIdAndRoleAndItemKey(companyId, normalizedRole, item.getItemKey());

            TenantNavigationConfigEntity entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.setVisible(item.isVisible());
                entity.setDisplayOrder(item.getDisplayOrder());
                entity.setCustomLabel(item.getCustomLabel());
                entity.setLocked(item.isLocked());
            } else {
                entity = TenantNavigationConfigEntity.builder()
                        .companyId(companyId)
                        .role(normalizedRole)
                        .itemKey(item.getItemKey())
                        .isVisible(item.isVisible())
                        .displayOrder(item.getDisplayOrder())
                        .customLabel(item.getCustomLabel())
                        .isLocked(item.isLocked())
                        .build();
            }
            entity = navigationConfigRepository.save(entity);

            savedResults.add(NavigationConfigDto.builder()
                    .itemKey(entity.getItemKey())
                    .isVisible(entity.isVisible())
                    .displayOrder(entity.getDisplayOrder())
                    .customLabel(entity.getCustomLabel())
                    .isLocked(entity.isLocked())
                    .role(entity.getRole())
                    .build());
        }

        return savedResults;
    }
}
