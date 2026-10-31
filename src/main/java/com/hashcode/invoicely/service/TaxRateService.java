package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.TaxRateDto;
import com.hashcode.invoicely.entity.relational.TaxRateEntity;

import java.util.UUID;

public interface TaxRateService {

    GenericManyResponse<TaxRateDto> getTaxRates();

    TaxRateDto getTaxRateById(UUID id);

    TaxRateDto createTaxRate(TaxRateDto dto);

    TaxRateDto updateTaxRate(UUID id, TaxRateDto dto);

    void deleteTaxRate(UUID id);

    TaxRateDto toDto(TaxRateEntity entity);
}
