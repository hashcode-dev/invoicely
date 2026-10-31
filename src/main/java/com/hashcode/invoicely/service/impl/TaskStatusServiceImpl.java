package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.TaskStatusDto;
import com.hashcode.invoicely.entity.relational.TaskStatusEntity;
import com.hashcode.invoicely.mapper.TaskStatusMapper;
import com.hashcode.invoicely.repository.relational.TaskStatusRepository;
import com.hashcode.invoicely.service.TaskStatusService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusServiceImpl implements TaskStatusService {

    private final TaskStatusRepository taskStatusRepository;
    private final TaskStatusMapper taskStatusMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<TaskStatusDto> getTaskStatuses(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 100;

        Sort sorting = Sort.by(Sort.Direction.ASC, "sortOrder");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<TaskStatusEntity> spec = (root, query, cb) -> {
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
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TaskStatusEntity> pageResult = taskStatusRepository.findAll(spec, pageable);
        List<TaskStatusDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatusDto getTaskStatusById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        TaskStatusEntity entity = taskStatusRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Task status not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public TaskStatusDto createTaskStatus(TaskStatusDto dto) {
        UUID companyId = getRequiredCompanyId();
        TaskStatusEntity entity = TaskStatusEntity.builder()
                .companyId(companyId)
                .name(dto.getName())
                .color(dto.getColor())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .statusOrder(dto.getStatusOrder() != null ? dto.getStatusOrder() : 0)
                .isDeleted(false)
                .build();

        TaskStatusEntity saved = taskStatusRepository.save(entity);
        log.info("Created task status {} ({}) for company {}", saved.getName(), saved.getId(), companyId);
        return toDto(saved);
    }

    @Override
    @Transactional
    public TaskStatusDto updateTaskStatus(UUID id, TaskStatusDto dto) {
        UUID companyId = getRequiredCompanyId();
        TaskStatusEntity entity = taskStatusRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Task status not found: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName());
        }
        if (dto.getColor() != null) {
            entity.setColor(dto.getColor());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatusOrder() != null) {
            entity.setStatusOrder(dto.getStatusOrder());
        }

        TaskStatusEntity saved = taskStatusRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteTaskStatus(UUID id) {
        UUID companyId = getRequiredCompanyId();
        TaskStatusEntity entity = taskStatusRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Task status not found: " + id));
        entity.setIsDeleted(true);
        taskStatusRepository.save(entity);
        log.info("Soft deleted task status {} for company {}", id, companyId);
    }

    @Override
    public TaskStatusDto toDto(TaskStatusEntity entity) {
        return taskStatusMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("TenantContext missing company ID");
        }
        return companyId;
    }
}
