package com.hashcode.invoicely.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleDto;
import com.hashcode.invoicely.dto.banking.RuleCriteriaDto;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.entity.relational.BankTransactionRuleEntity;
import com.hashcode.invoicely.repository.relational.ExpenseCategoryRepository;
import com.hashcode.invoicely.repository.relational.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankTransactionRuleMapper {

    private final ObjectMapper objectMapper;
    private final VendorRepository vendorRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final VendorMapper vendorMapper;
    private final ExpenseCategoryMapper expenseCategoryMapper;

    public BankTransactionRuleDto toDto(BankTransactionRuleEntity entity) {
        if (entity == null) {
            return null;
        }

        VendorDto vendorDto = null;
        if (entity.getVendorId() != null) {
            try {
                vendorDto = vendorRepository.findById(entity.getVendorId()).map(vendorMapper::toDto).orElse(null);
            } catch (Exception e) {
                log.debug("Vendor {} not found for rule {}", entity.getVendorId(), entity.getId());
            }
        }

        ExpenseCategoryDto categoryDto = null;
        if (entity.getCategoryId() != null) {
            try {
                categoryDto = expenseCategoryRepository.findById(entity.getCategoryId()).map(expenseCategoryMapper::toDto).orElse(null);
            } catch (Exception e) {
                log.debug("Category {} not found for rule {}", entity.getCategoryId(), entity.getId());
            }
        }

        return toDto(entity, vendorDto, categoryDto);
    }

    public BankTransactionRuleDto toDto(BankTransactionRuleEntity entity, VendorDto vendorDto, ExpenseCategoryDto categoryDto) {
        if (entity == null) {
            return null;
        }
        return BankTransactionRuleDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .appliesTo(entity.getAppliesTo())
                .rules(parseRules(entity.getRules()))
                .autoConvert(Boolean.TRUE.equals(entity.getAutoConvert()))
                .matchesOnAll(Boolean.TRUE.equals(entity.getMatchesOnAll()))
                .vendorId(entity.getVendorId())
                .categoryId(entity.getCategoryId())
                .clientId(entity.getClientId())
                .vendor(vendorDto)
                .expenseCategory(categoryDto)
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    public List<RuleCriteriaDto> parseRules(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RuleCriteriaDto>>() {});
        } catch (Exception e) {
            log.warn("Could not parse rule criteria JSON: '{}'", json);
            return new ArrayList<>();
        }
    }

    public String serializeRules(List<RuleCriteriaDto> rules) {
        if (rules == null || rules.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("Failed to serialize rule criteria", e);
            return "[]";
        }
    }
}
