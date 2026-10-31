package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.ProjectBulkActionRequest;
import com.hashcode.invoicely.dto.project.ProjectDto;
import com.hashcode.invoicely.entity.relational.ProjectEntity;

import java.util.UUID;

public interface ProjectService {

    GenericManyResponse<ProjectDto> getProjects(int page, int perPage, String status, UUID clientId, String filter, String sort);

    ProjectDto getProjectById(UUID id);

    ProjectDto getBlankProject();

    ProjectDto createProject(ProjectDto dto);

    ProjectDto updateProject(UUID id, ProjectDto dto);

    void deleteProject(UUID id);

    int handleBulkAction(ProjectBulkActionRequest request);

    ProjectDto toDto(ProjectEntity entity);
}
