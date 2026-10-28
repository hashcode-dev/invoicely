package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.entity.relational.ExpenseCategoryEntity;
import com.hashcode.invoicely.repository.relational.ExpenseCategoryRepository;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Transactional(readOnly = true)
    public GenericManyResponse<ExpenseCategoryDto> getExpenseCategories(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 100;

        Sort sorting = Sort.by(Sort.Direction.ASC, "name");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<ExpenseCategoryEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ExpenseCategoryEntity> paged = expenseCategoryRepository.findAll(spec, pageable);
        List<ExpenseCategoryDto> dtos = paged.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, paged.getTotalElements(), paged.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public ExpenseCategoryDto getExpenseCategoryById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ExpenseCategoryEntity entity = expenseCategoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public ExpenseCategoryDto createExpenseCategory(ExpenseCategoryDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("Expense category name is required");
        }

        ExpenseCategoryEntity entity = ExpenseCategoryEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .name(dto.getName().trim())
                .color(dto.getColor())
                .isDeleted(false)
                .build();

        ExpenseCategoryEntity saved = expenseCategoryRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public ExpenseCategoryDto updateExpenseCategory(UUID id, ExpenseCategoryDto dto) {
        UUID companyId = getRequiredCompanyId();
        ExpenseCategoryEntity entity = expenseCategoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getColor() != null) {
            entity.setColor(dto.getColor().trim());
        }

        ExpenseCategoryEntity updated = expenseCategoryRepository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public void deleteExpenseCategory(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ExpenseCategoryEntity entity = expenseCategoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));
        entity.setDeleted(true);
        expenseCategoryRepository.save(entity);
    }

    @Transactional
    public List<ExpenseCategoryDto> bulkAction(ExpenseCategoryBulkActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();
        }
        UUID companyId = getRequiredCompanyId();
        List<ExpenseCategoryEntity> entities = expenseCategoryRepository.findByCompanyIdAndIdIn(companyId, request.getIds());

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        for (ExpenseCategoryEntity entity : entities) {
            switch (action) {
                case "archive" -> entity.setArchivedAt(Instant.now());
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setDeleted(false);
                }
                case "delete" -> entity.setDeleted(true);
                default -> throw new IllegalArgumentException("Unsupported bulk action: " + action);
            }
        }
        expenseCategoryRepository.saveAll(entities);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ExpenseCategoryDto toDto(ExpenseCategoryEntity entity) {
        if (entity == null) return null;
        return ExpenseCategoryDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .color(entity.getColor())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
