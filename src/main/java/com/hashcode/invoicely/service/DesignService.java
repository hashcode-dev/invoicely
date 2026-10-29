package com.hashcode.invoicely.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.DesignBulkActionRequest;
import com.hashcode.invoicely.dto.document.DesignDto;
import com.hashcode.invoicely.dto.document.PartsDto;
import com.hashcode.invoicely.dto.document.SetDefaultDesignRequest;
import com.hashcode.invoicely.entity.relational.DesignEntity;
import com.hashcode.invoicely.repository.relational.DesignRepository;
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
public class DesignService {

    private final DesignRepository designRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public GenericManyResponse<DesignDto> getDesigns(int page, int perPage, String status, Boolean custom, Boolean template, String entities, String filter, String sort) {
        UUID companyId = TenantContext.getCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.ASC, "name");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<DesignEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant scope: either belongs to tenant company OR is a system-wide template (companyId is null)
            if (companyId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("companyId"), companyId),
                        cb.isNull(root.get("companyId"))
                ));
            }

            if (custom != null) {
                predicates.add(cb.equal(root.get("isCustom"), custom));
            }
            if (template != null) {
                predicates.add(cb.equal(root.get("isTemplate"), template));
            }
            if (StringUtils.hasText(entities)) {
                predicates.add(cb.like(cb.lower(root.get("entities")), "%" + entities.toLowerCase() + "%"));
            }

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

        Page<DesignEntity> pageResult = designRepository.findAll(spec, pageable);
        List<DesignDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public DesignDto getDesignById(UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        DesignEntity entity = designRepository.findById(id)
                .filter(d -> d.getCompanyId() == null || (companyId != null && companyId.equals(d.getCompanyId())))
                .orElseThrow(() -> new IllegalArgumentException("Design not found: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public DesignDto getBlankDesign() {
        PartsDto defaultParts = PartsDto.builder()
                .header("<header><h1>$company.name</h1><p>$invoice.number</p></header>")
                .body("<main>$invoice.details</main>")
                .product("<table>$invoice.line_items</table>")
                .task("<table>$invoice.task_items</table>")
                .footer("<footer>$company.address</footer>")
                .includes("<style>body { font-family: Inter, sans-serif; color: #111; }</style>")
                .build();

        return DesignDto.builder()
                .name("New Custom Design")
                .design(defaultParts)
                .isCustom(true)
                .isActive(true)
                .isDeleted(false)
                .isFree(true)
                .isTemplate(false)
                .entities("invoice")
                .build();
    }

    @Transactional
    public DesignDto createDesign(DesignDto dto) {
        UUID companyId = TenantContext.getCompanyId();
        UUID userId = TenantContext.getUserId();

        String name = StringUtils.hasText(dto.getName()) ? dto.getName().trim() : "Custom Design";
        String serializedDesign = serializeParts(dto.getDesign());

        DesignEntity entity = DesignEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .name(name)
                .design(serializedDesign)
                .isCustom(true)
                .isActive(dto.isActive())
                .isDeleted(false)
                .isFree(dto.isFree())
                .isTemplate(dto.isTemplate())
                .entities(StringUtils.hasText(dto.getEntities()) ? dto.getEntities() : "invoice")
                .build();

        DesignEntity saved = designRepository.save(entity);
        log.info("Created custom design {} ({}) for company {}", saved.getName(), saved.getId(), companyId);
        return toDto(saved);
    }

    @Transactional
    public DesignDto updateDesign(UUID id, DesignDto dto) {
        UUID companyId = TenantContext.getCompanyId();
        DesignEntity entity = designRepository.findById(id)
                .filter(d -> companyId != null && companyId.equals(d.getCompanyId()))
                .orElseThrow(() -> new IllegalArgumentException("Custom design not found: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getDesign() != null) {
            entity.setDesign(serializeParts(dto.getDesign()));
        }
        entity.setIsActive(dto.isActive());
        if (dto.getEntities() != null) {
            entity.setEntities(dto.getEntities());
        }

        DesignEntity saved = designRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteDesign(UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        DesignEntity entity = designRepository.findById(id)
                .filter(d -> companyId != null && companyId.equals(d.getCompanyId()))
                .orElseThrow(() -> new IllegalArgumentException("Design not found: " + id));
        entity.setIsDeleted(true);
        designRepository.save(entity);
        log.info("Soft deleted design {} for company {}", id, companyId);
    }

    @Transactional
    public int handleBulkAction(DesignBulkActionRequest request) {
        UUID companyId = TenantContext.getCompanyId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }

        List<DesignEntity> entities = designRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        for (DesignEntity entity : entities) {
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
                default -> log.warn("Unknown design bulk action: {}", action);
            }
        }

        designRepository.saveAll(entities);
        return entities.size();
    }

    @Transactional
    public boolean setDefaultDesign(SetDefaultDesignRequest request) {
        log.info("Set default design {} for entity {}", request.getDesignId(), request.getEntity());
        return true;
    }

    public DesignDto toDto(DesignEntity entity) {
        if (entity == null) return null;
        return DesignDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .design(parseParts(entity.getDesign()))
                .isCustom(Boolean.TRUE.equals(entity.getIsCustom()))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .isFree(Boolean.TRUE.equals(entity.getIsFree()))
                .isTemplate(Boolean.TRUE.equals(entity.getIsTemplate()))
                .entities(entity.getEntities())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .build();
    }

    private String serializeParts(PartsDto parts) {
        if (parts == null) return "{}";
        try {
            return objectMapper.writeValueAsString(parts);
        } catch (Exception e) {
            return "{}";
        }
    }

    private PartsDto parseParts(String json) {
        if (!StringUtils.hasText(json)) return PartsDto.builder().build();
        try {
            return objectMapper.readValue(json, PartsDto.class);
        } catch (Exception e) {
            return PartsDto.builder().build();
        }
    }
}
