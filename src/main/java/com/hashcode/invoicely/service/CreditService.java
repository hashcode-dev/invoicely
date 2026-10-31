package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.CreditDto;
import com.hashcode.invoicely.entity.relational.CreditEntity;

import java.util.UUID;

public interface CreditService {

    GenericManyResponse<CreditDto> getCredits(int page, int perPage, String status, UUID clientId, String filter);

    CreditDto getBlankCredit(UUID clientId);

    CreditDto getCreditById(UUID id);

    CreditDto createCredit(CreditDto dto);

    CreditDto updateCredit(UUID id, CreditDto dto);

    CreditDto toDto(CreditEntity entity);
}
