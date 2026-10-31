package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.ProjectBulkActionRequest;
import com.hashcode.invoicely.dto.project.ProjectDto;
import com.hashcode.invoicely.entity.relational.ProjectEntity;
import com.hashcode.invoicely.mapper.ProjectMapper;
import com.hashcode.invoicely.repository.relational.ProjectRepository;
import com.hashcode.invoicely.service.ProjectService;
import com.hashcode.invoicely.service.SequenceGeneratorService;
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
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<ProjectDto> getProjects(int page, int perPage, String status, UUID clientId, String filter, String sort) {
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

        Specification<ProjectEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
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

        Page<ProjectEntity> pageResult = projectRepository.findAll(spec, pageable);
        List<ProjectDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ProjectEntity entity = projectRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getBlankProject() {
        return ProjectDto.builder()
                .taskRate(BigDecimal.ZERO)
                .budgetedHours(BigDecimal.ZERO)
                .currentHours(BigDecimal.ZERO)
                .color("#004bca")
                .isDeleted(false)
                .build();
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String projectNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("project_" + companyId);

        String name = StringUtils.hasText(dto.getName()) ? dto.getName().trim() : "New Project";

        ProjectEntity entity = ProjectEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .clientId(dto.getClientId())
                .name(name)
                .number(projectNumber)
                .taskRate(dto.getTaskRate() != null ? dto.getTaskRate() : BigDecimal.ZERO)
                .budgetedHours(dto.getBudgetedHours() != null ? dto.getBudgetedHours() : BigDecimal.ZERO)
                .dueDate(dto.getDueDate())
                .privateNotes(dto.getPrivateNotes())
                .publicNotes(dto.getPublicNotes())
                .color(dto.getColor())
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .isDeleted(false)
                .build();

        ProjectEntity saved = projectRepository.save(entity);
        log.info("Created project {} ({}) for company {}", saved.getName(), saved.getId(), companyId);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(UUID id, ProjectDto dto) {
        UUID companyId = getRequiredCompanyId();
        ProjectEntity entity = projectRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName());
        }
        if (dto.getClientId() != null) {
            entity.setClientId(dto.getClientId());
        }
        if (dto.getAssignedUserId() != null) {
            entity.setAssignedUserId(dto.getAssignedUserId());
        }
        if (dto.getTaskRate() != null) {
            entity.setTaskRate(dto.getTaskRate());
        }
        if (dto.getBudgetedHours() != null) {
            entity.setBudgetedHours(dto.getBudgetedHours());
        }
        if (dto.getDueDate() != null) {
            entity.setDueDate(dto.getDueDate());
        }
        if (dto.getPrivateNotes() != null) {
            entity.setPrivateNotes(dto.getPrivateNotes());
        }
        if (dto.getPublicNotes() != null) {
            entity.setPublicNotes(dto.getPublicNotes());
        }
        if (dto.getColor() != null) {
            entity.setColor(dto.getColor());
        }
        if (dto.getCustomValue1() != null) {
            entity.setCustomValue1(dto.getCustomValue1());
        }
        if (dto.getCustomValue2() != null) {
            entity.setCustomValue2(dto.getCustomValue2());
        }
        if (dto.getCustomValue3() != null) {
            entity.setCustomValue3(dto.getCustomValue3());
        }
        if (dto.getCustomValue4() != null) {
            entity.setCustomValue4(dto.getCustomValue4());
        }

        ProjectEntity saved = projectRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteProject(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ProjectEntity entity = projectRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        entity.setIsDeleted(true);
        projectRepository.save(entity);
        log.info("Soft deleted project {} for company {}", id, companyId);
    }

    @Override
    @Transactional
    public int handleBulkAction(ProjectBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }

        List<ProjectEntity> projects = projectRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        for (ProjectEntity project : projects) {
            switch (action) {
                case "archive" -> {
                    project.setArchivedAt(Instant.now());
                    project.setIsDeleted(false);
                }
                case "restore" -> {
                    project.setArchivedAt(null);
                    project.setIsDeleted(false);
                }
                case "delete" -> project.setIsDeleted(true);
                default -> log.warn("Unknown project bulk action: {}", action);
            }
        }

        projectRepository.saveAll(projects);
        return projects.size();
    }

    @Override
    public ProjectDto toDto(ProjectEntity entity) {
        return projectMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("TenantContext missing company ID");
        }
        return companyId;
    }
}
