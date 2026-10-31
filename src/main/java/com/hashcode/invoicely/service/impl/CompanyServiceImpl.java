package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.company.CreateCompanyRequest;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.mapper.CompanyMapper;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<CompanyDto> getCompanies(int page, int perPage) {
        UUID userId = TenantContext.getUserId();
        List<CompanyUserEntity> links = companyUserRepository.findByUserId(userId);

        List<CompanyDto> dtoList = new ArrayList<>();
        for (CompanyUserEntity link : links) {
            companyRepository.findById(link.getCompanyId())
                    .filter(c -> !c.isDeleted())
                    .ifPresent(c -> dtoList.add(companyMapper.toDto(c)));
        }

        return GenericManyResponse.of(dtoList, dtoList.size(), page, perPage);
    }

    @Override
    @Transactional
    public ApiResponse<CompanyDto> createCompany(CreateCompanyRequest request) {
        UUID accountId = TenantContext.getAccountId();
        UUID userId = TenantContext.getUserId();

        String companyName = (request != null && request.getCompanyName() != null && !request.getCompanyName().isBlank())
                ? request.getCompanyName().trim()
                : "New Company";

        String subdomain = (request != null && request.getSubdomain() != null && !request.getSubdomain().isBlank())
                ? request.getSubdomain()
                : "comp-" + UUID.randomUUID().toString().substring(0, 8);

        CompanyEntity company = CompanyEntity.builder()
                .accountId(accountId)
                .companyName(companyName)
                .subdomain(subdomain)
                .currencyId(request != null && request.getCurrencyId() != null ? request.getCurrencyId() : "USD")
                .countryId(request != null && request.getCountryId() != null ? request.getCountryId() : "US")
                .sizeId(request.getSizeId())
                .industryId(request.getIndustryId())
                .vatNumber(request.getVatNumber())
                .gstNumber(request.getGstNumber())
                .build();
        company = companyRepository.save(company);

        CompanyUserEntity link = CompanyUserEntity.builder()
                .companyId(company.getId())
                .userId(userId)
                .accountId(accountId)
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .build();
        companyUserRepository.save(link);

        return ApiResponse.of(companyMapper.toDto(company));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CompanyDto> getCompany(UUID id) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        return ApiResponse.of(companyMapper.toDto(company));
    }

    @Override
    @Transactional
    public ApiResponse<CompanyDto> updateCompany(UUID id, CreateCompanyRequest request) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setCompanyName(request.getCompanyName());
        if (request.getSubdomain() != null && !request.getSubdomain().isBlank()) {
            company.setSubdomain(request.getSubdomain());
        }
        if (request.getCurrencyId() != null) {
            company.setCurrencyId(request.getCurrencyId());
        }
        if (request.getCountryId() != null) {
            company.setCountryId(request.getCountryId());
        }
        company.setSizeId(request.getSizeId());
        company.setIndustryId(request.getIndustryId());
        company.setVatNumber(request.getVatNumber());
        company.setGstNumber(request.getGstNumber());

        company = companyRepository.save(company);
        return ApiResponse.of(companyMapper.toDto(company));
    }
}
