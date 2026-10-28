package com.hashcode.invoicely.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleDto;
import com.hashcode.invoicely.dto.banking.RuleCriteriaDto;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.entity.relational.BankTransactionEntity;
import com.hashcode.invoicely.entity.relational.BankTransactionRuleEntity;
import com.hashcode.invoicely.repository.relational.BankTransactionRuleRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransactionRuleService {

    private final BankTransactionRuleRepository ruleRepository;
    private final VendorService vendorService;
    private final ExpenseCategoryService expenseCategoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public GenericManyResponse<BankTransactionRuleDto> getRules(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<BankTransactionRuleEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if ("all".equalsIgnoreCase(status)) {
                // all
            } else {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BankTransactionRuleEntity> pageResult = ruleRepository.findAll(spec, pageable);
        List<BankTransactionRuleDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public BankTransactionRuleDto getRuleById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionRuleEntity entity = ruleRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction rule not found: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public BankTransactionRuleDto getBlankRule() {
        return BankTransactionRuleDto.builder()
                .appliesTo("all")
                .rules(new ArrayList<>())
                .autoConvert(false)
                .matchesOnAll(true)
                .isDeleted(false)
                .build();
    }

    @Transactional
    public BankTransactionRuleDto createRule(BankTransactionRuleDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String serializedRules = serializeRules(dto.getRules());

        BankTransactionRuleEntity entity = BankTransactionRuleEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .name(StringUtils.hasText(dto.getName()) ? dto.getName().trim() : "New Rule")
                .appliesTo(StringUtils.hasText(dto.getAppliesTo()) ? dto.getAppliesTo() : "all")
                .rules(serializedRules)
                .autoConvert(dto.isAutoConvert())
                .matchesOnAll(dto.isMatchesOnAll())
                .vendorId(dto.getVendorId())
                .categoryId(dto.getCategoryId())
                .clientId(dto.getClientId())
                .isDeleted(false)
                .build();

        BankTransactionRuleEntity saved = ruleRepository.save(entity);
        log.info("Created bank transaction rule {} ({}) for company {}", saved.getName(), saved.getId(), companyId);
        return toDto(saved);
    }

    @Transactional
    public BankTransactionRuleDto updateRule(UUID id, BankTransactionRuleDto dto) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionRuleEntity entity = ruleRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction rule not found: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName());
        }
        if (dto.getAppliesTo() != null) {
            entity.setAppliesTo(dto.getAppliesTo());
        }
        if (dto.getRules() != null) {
            entity.setRules(serializeRules(dto.getRules()));
        }
        entity.setAutoConvert(dto.isAutoConvert());
        entity.setMatchesOnAll(dto.isMatchesOnAll());
        if (dto.getVendorId() != null) {
            entity.setVendorId(dto.getVendorId());
        }
        if (dto.getCategoryId() != null) {
            entity.setCategoryId(dto.getCategoryId());
        }
        if (dto.getClientId() != null) {
            entity.setClientId(dto.getClientId());
        }

        BankTransactionRuleEntity saved = ruleRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteRule(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionRuleEntity entity = ruleRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction rule not found: " + id));
        entity.setIsDeleted(true);
        ruleRepository.save(entity);
        log.info("Soft deleted bank transaction rule {} for company {}", id, companyId);
    }

    @Transactional
    public int handleBulkAction(BankTransactionRuleBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }

        List<BankTransactionRuleEntity> entities = ruleRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        for (BankTransactionRuleEntity entity : entities) {
            switch (action) {
                case "archive" -> {
                    entity.setArchivedAt(Instant.now());
                    entity.setIsDeleted(false);
                }
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setIsDeleted(false);
                }
                case "delete" -> entity.setIsDeleted(true);
                default -> log.warn("Unknown rule bulk action: {}", action);
            }
        }

        ruleRepository.saveAll(entities);
        return entities.size();
    }

    public boolean matches(BankTransactionRuleEntity rule, BankTransactionEntity transaction) {
        if (rule == null || transaction == null) return false;
        List<RuleCriteriaDto> criteriaList = parseRules(rule.getRules());
        if (criteriaList.isEmpty()) return false;

        boolean matchesOnAll = Boolean.TRUE.equals(rule.getMatchesOnAll());
        for (RuleCriteriaDto criteria : criteriaList) {
            boolean singleMatch = evaluateCriteria(criteria, transaction);
            if (matchesOnAll && !singleMatch) {
                return false;
            }
            if (!matchesOnAll && singleMatch) {
                return true;
            }
        }
        return matchesOnAll;
    }

    private boolean evaluateCriteria(RuleCriteriaDto criteria, BankTransactionEntity tx) {
        if (criteria == null || !StringUtils.hasText(criteria.getSearchKey()) || criteria.getValue() == null) {
            return false;
        }

        String searchKey = criteria.getSearchKey().toLowerCase();
        String operator = StringUtils.hasText(criteria.getOperator()) ? criteria.getOperator().toLowerCase() : "contains";
        String targetVal = criteria.getValue().toLowerCase().trim();

        String actualVal = "";
        if ("description".equals(searchKey)) {
            actualVal = tx.getDescription() != null ? tx.getDescription().toLowerCase() : "";
        } else if ("participant".equals(searchKey) || "participant_name".equals(searchKey)) {
            actualVal = tx.getParticipant() != null ? tx.getParticipant().toLowerCase() : "";
        } else if ("amount".equals(searchKey)) {
            BigDecimal actualAmount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            try {
                BigDecimal targetAmount = new BigDecimal(targetVal);
                return switch (operator) {
                    case "equals" -> actualAmount.compareTo(targetAmount) == 0;
                    case "greater_than" -> actualAmount.compareTo(targetAmount) > 0;
                    case "less_than" -> actualAmount.compareTo(targetAmount) < 0;
                    default -> false;
                };
            } catch (Exception e) {
                return false;
            }
        }

        return switch (operator) {
            case "equals" -> actualVal.equals(targetVal);
            case "starts_with" -> actualVal.startsWith(targetVal);
            case "ends_with" -> actualVal.endsWith(targetVal);
            default -> actualVal.contains(targetVal);
        };
    }

    public BankTransactionRuleDto toDto(BankTransactionRuleEntity entity) {
        if (entity == null) return null;

        VendorDto vendorDto = null;
        if (entity.getVendorId() != null) {
            try {
                vendorDto = vendorService.getVendorById(entity.getVendorId());
            } catch (Exception e) {
                log.debug("Vendor {} not found for rule {}", entity.getVendorId(), entity.getId());
            }
        }

        ExpenseCategoryDto categoryDto = null;
        if (entity.getCategoryId() != null) {
            try {
                categoryDto = expenseCategoryService.getExpenseCategoryById(entity.getCategoryId());
            } catch (Exception e) {
                log.debug("Category {} not found for rule {}", entity.getCategoryId(), entity.getId());
            }
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

    private List<RuleCriteriaDto> parseRules(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<RuleCriteriaDto>>() {});
        } catch (Exception e) {
            log.warn("Could not parse rule criteria JSON: '{}'", json);
            return new ArrayList<>();
        }
    }

    private String serializeRules(List<RuleCriteriaDto> rules) {
        if (rules == null || rules.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("Failed to serialize rule criteria", e);
            return "[]";
        }
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
