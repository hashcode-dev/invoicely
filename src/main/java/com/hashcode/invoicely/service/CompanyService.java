package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.company.CreateCompanyRequest;

import java.util.UUID;

public interface CompanyService {

    GenericManyResponse<CompanyDto> getCompanies(int page, int perPage);

    ApiResponse<CompanyDto> createCompany(CreateCompanyRequest request);

    ApiResponse<CompanyDto> getCompany(UUID id);

    ApiResponse<CompanyDto> updateCompany(UUID id, CreateCompanyRequest request);
}
