package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.entity.relational.ExpenseCategoryEntity;
import com.hashcode.invoicely.mapper.ExpenseCategoryMapper;
import com.hashcode.invoicely.repository.relational.ExpenseCategoryRepository;
import com.hashcode.invoicely.service.ExpenseCategoryService;
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
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseCategoryMapper expenseCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<ExpenseCategoryDto> getExpenseCategories(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 500;

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

    @Override
    @Transactional(readOnly = true)
    public ExpenseCategoryDto getExpenseCategoryById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ExpenseCategoryEntity entity = expenseCategoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public ExpenseCategoryDto createExpenseCategory(ExpenseCategoryDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        ExpenseCategoryEntity entity = ExpenseCategoryEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .name(dto.getName() != null ? dto.getName().trim() : "")
                .color(dto.getColor())
                .isDeleted(false)
                .build();

        ExpenseCategoryEntity saved = expenseCategoryRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseCategoryDto updateExpenseCategory(UUID id, ExpenseCategoryDto dto) {
        UUID companyId = getRequiredCompanyId();
        ExpenseCategoryEntity entity = expenseCategoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));

        if (dto.getName() != null) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getColor() != null) {
            entity.setColor(dto.getColor());
        }

        ExpenseCategoryEntity updated = expenseCategoryRepository.save(entity);
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteExpenseCategory(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ExpenseCategoryEntity entity = expenseCategoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));
        entity.setDeleted(true);
        expenseCategoryRepository.save(entity);
    }

    @Override
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

    @Override
    public ExpenseCategoryDto toDto(ExpenseCategoryEntity entity) {
        return expenseCategoryMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
