package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyDto toDto(CompanyEntity entity) {
        if (entity == null) {
            return null;
        }
        return CompanyDto.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .subdomain(entity.getSubdomain())
                .currencyId(entity.getCurrencyId())
                .countryId(entity.getCountryId())
                .sizeId(entity.getSizeId())
                .industryId(entity.getIndustryId())
                .vatNumber(entity.getVatNumber())
                .gstNumber(entity.getGstNumber())
                .portalDomain(entity.getPortalDomain())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
