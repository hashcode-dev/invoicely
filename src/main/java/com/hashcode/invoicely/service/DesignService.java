package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.DesignBulkActionRequest;
import com.hashcode.invoicely.dto.document.DesignDto;
import com.hashcode.invoicely.dto.document.SetDefaultDesignRequest;
import com.hashcode.invoicely.entity.relational.DesignEntity;

import java.util.UUID;

public interface DesignService {

    GenericManyResponse<DesignDto> getDesigns(int page, int perPage, String status, Boolean custom, Boolean template, String entities, String filter, String sort);

    DesignDto getDesignById(UUID id);

    DesignDto getBlankDesign();

    DesignDto createDesign(DesignDto dto);

    DesignDto updateDesign(UUID id, DesignDto dto);

    void deleteDesign(UUID id);

    int handleBulkAction(DesignBulkActionRequest request);

    boolean setDefaultDesign(SetDefaultDesignRequest request);

    DesignDto toDto(DesignEntity entity);
}
