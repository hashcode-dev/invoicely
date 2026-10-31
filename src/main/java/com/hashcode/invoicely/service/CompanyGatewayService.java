package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.payment.CompanyGatewayBulkActionRequest;
import com.hashcode.invoicely.dto.payment.CompanyGatewayDto;
import com.hashcode.invoicely.entity.relational.CompanyGatewayEntity;

import java.util.List;
import java.util.UUID;

public interface CompanyGatewayService {

    GenericManyResponse<CompanyGatewayDto> getCompanyGateways(int page, int perPage, String status, String filter, String sort);

    CompanyGatewayDto getBlankCompanyGateway();

    CompanyGatewayDto getCompanyGatewayById(UUID id);

    CompanyGatewayDto createCompanyGateway(CompanyGatewayDto dto);

    CompanyGatewayDto updateCompanyGateway(UUID id, CompanyGatewayDto dto);

    void deleteCompanyGateway(UUID id);

    List<CompanyGatewayDto> bulkAction(CompanyGatewayBulkActionRequest request);

    CompanyGatewayDto toDto(CompanyGatewayEntity entity);
}
