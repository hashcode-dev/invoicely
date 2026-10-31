package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.payment.CompanyGatewayBulkActionRequest;
import com.hashcode.invoicely.dto.payment.CompanyGatewayDto;
import com.hashcode.invoicely.entity.relational.CompanyGatewayEntity;
import com.hashcode.invoicely.mapper.CompanyGatewayMapper;
import com.hashcode.invoicely.repository.relational.CompanyGatewayRepository;
import com.hashcode.invoicely.service.CompanyGatewayService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyGatewayServiceImpl implements CompanyGatewayService {

    private final CompanyGatewayRepository companyGatewayRepository;
    private final CompanyGatewayMapper companyGatewayMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<CompanyGatewayDto> getCompanyGateways(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.ASC, "label");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<CompanyGatewayEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status) || "deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("gatewayKey")), pattern),
                        cb.like(cb.lower(root.get("label")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CompanyGatewayEntity> paged = companyGatewayRepository.findAll(spec, pageable);
        List<CompanyGatewayDto> dtos = paged.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, paged.getTotalElements(), paged.getTotalPages(), pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyGatewayDto getBlankCompanyGateway() {
        return CompanyGatewayDto.builder()
                .gatewayKey("")
                .label("")
                .config("{}")
                .isDeleted(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyGatewayDto getCompanyGatewayById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        CompanyGatewayEntity entity = companyGatewayRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Company gateway not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public CompanyGatewayDto createCompanyGateway(CompanyGatewayDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        CompanyGatewayEntity entity = CompanyGatewayEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .gatewayKey(dto.getGatewayKey() != null ? dto.getGatewayKey().trim() : "custom")
                .label(dto.getLabel() != null ? dto.getLabel().trim() : "Payment Gateway")
                .config(dto.getConfig())
                .isDeleted(false)
                .build();

        CompanyGatewayEntity saved = companyGatewayRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CompanyGatewayDto updateCompanyGateway(UUID id, CompanyGatewayDto dto) {
        UUID companyId = getRequiredCompanyId();
        CompanyGatewayEntity entity = companyGatewayRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Company gateway not found: " + id));

        if (dto.getGatewayKey() != null) {
            entity.setGatewayKey(dto.getGatewayKey().trim());
        }
        if (dto.getLabel() != null) {
            entity.setLabel(dto.getLabel().trim());
        }
        if (dto.getConfig() != null) {
            entity.setConfig(dto.getConfig());
        }
        if (dto.isDeleted() != entity.isDeleted()) {
            entity.setDeleted(dto.isDeleted());
        }

        CompanyGatewayEntity updated = companyGatewayRepository.save(entity);
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteCompanyGateway(UUID id) {
        UUID companyId = getRequiredCompanyId();
        CompanyGatewayEntity entity = companyGatewayRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Company gateway not found: " + id));
        entity.setDeleted(true);
        companyGatewayRepository.save(entity);
    }

    @Override
    @Transactional
    public List<CompanyGatewayDto> bulkAction(CompanyGatewayBulkActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();
        }
        UUID companyId = getRequiredCompanyId();
        List<CompanyGatewayEntity> entities = companyGatewayRepository.findByCompanyIdAndIdIn(companyId, request.getIds());

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        for (CompanyGatewayEntity entity : entities) {
            switch (action) {
                case "archive", "delete" -> entity.setDeleted(true);
                case "restore" -> entity.setDeleted(false);
                default -> throw new IllegalArgumentException("Unsupported bulk action: " + action);
            }
        }
        companyGatewayRepository.saveAll(entities);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public CompanyGatewayDto toDto(CompanyGatewayEntity entity) {
        return companyGatewayMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
