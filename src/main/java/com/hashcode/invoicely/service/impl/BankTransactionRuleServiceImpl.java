package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleDto;
import com.hashcode.invoicely.dto.banking.RuleCriteriaDto;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.BankTransactionEntity;
import com.hashcode.invoicely.entity.relational.BankTransactionRuleEntity;
import com.hashcode.invoicely.mapper.BankTransactionRuleMapper;
import com.hashcode.invoicely.repository.relational.BankTransactionRuleRepository;
import com.hashcode.invoicely.service.BankTransactionRuleService;
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
public class BankTransactionRuleServiceImpl implements BankTransactionRuleService {

    private final BankTransactionRuleRepository ruleRepository;
    private final BankTransactionRuleMapper bankTransactionRuleMapper;

    @Override
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
            } else if (!"all".equalsIgnoreCase(status)) {
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

    @Override
    @Transactional(readOnly = true)
    public BankTransactionRuleDto getRuleById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionRuleEntity entity = ruleRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction rule not found: " + id));
        return toDto(entity);
    }

    @Override
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

    @Override
    @Transactional
    public BankTransactionRuleDto createRule(BankTransactionRuleDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String serializedRules = bankTransactionRuleMapper.serializeRules(dto.getRules());

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

    @Override
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
            entity.setRules(bankTransactionRuleMapper.serializeRules(dto.getRules()));
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

    @Override
    @Transactional
    public void deleteRule(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionRuleEntity entity = ruleRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction rule not found: " + id));
        entity.setIsDeleted(true);
        ruleRepository.save(entity);
        log.info("Soft deleted bank transaction rule {} for company {}", id, companyId);
    }

    @Override
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

    @Override
    public boolean matches(BankTransactionRuleEntity rule, BankTransactionEntity transaction) {
        if (rule == null || transaction == null) return false;
        List<RuleCriteriaDto> criteriaList = bankTransactionRuleMapper.parseRules(rule.getRules());
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

    @Override
    public BankTransactionRuleDto toDto(BankTransactionRuleEntity entity) {
        return bankTransactionRuleMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
