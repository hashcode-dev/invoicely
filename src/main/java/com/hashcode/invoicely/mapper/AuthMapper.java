package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.auth.AccountDto;
import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.dto.auth.CompanyUserResponse;
import com.hashcode.invoicely.dto.auth.TokenDto;
import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.entity.relational.AccountEntity;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthMapper {

    public UserDto toUserDto(UserEntity user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatar(user.getAvatarUrl())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .organizationId(user.getOrganizationId())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().getEpochSecond() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    public CompanyDto toCompanyDto(CompanyEntity company) {
        if (company == null) {
            return null;
        }
        return CompanyDto.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .subdomain(company.getSubdomain())
                .currencyId(company.getCurrencyId())
                .countryId(company.getCountryId())
                .sizeId(company.getSizeId())
                .industryId(company.getIndustryId())
                .vatNumber(company.getVatNumber())
                .gstNumber(company.getGstNumber())
                .portalDomain(company.getPortalDomain())
                .settings(java.util.Map.of("name", company.getCompanyName() != null ? company.getCompanyName() : ""))
                .createdAt(company.getCreatedAt() != null ? company.getCreatedAt().getEpochSecond() : null)
                .updatedAt(company.getUpdatedAt() != null ? company.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    public AccountDto toAccountDto(AccountEntity account, UUID defaultCompanyId) {
        if (account == null) {
            return null;
        }
        return AccountDto.builder()
                .id(account.getId())
                .defaultCompanyId(account.getDefaultCompanyId() != null ? account.getDefaultCompanyId() : defaultCompanyId)
                .plan(account.getPlan())
                .createdAt(account.getCreatedAt() != null ? account.getCreatedAt().getEpochSecond() : null)
                .updatedAt(account.getUpdatedAt() != null ? account.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    public TokenDto toTokenDto(UUID tokenId, String tokenString, UUID userId) {
        return TokenDto.builder()
                .id(tokenId != null ? tokenId : UUID.randomUUID())
                .token(tokenString)
                .name("Web Session")
                .userId(userId)
                .createdAt(System.currentTimeMillis() / 1000)
                .updatedAt(System.currentTimeMillis() / 1000)
                .build();
    }

    public CompanyUserResponse toCompanyUserResponse(
            UserDto userDto,
            CompanyDto companyDto,
            AccountDto accountDto,
            TokenDto tokenDto,
            CompanyUserEntity link
    ) {
        return CompanyUserResponse.builder()
                .user(userDto)
                .company(companyDto)
                .account(accountDto)
                .token(tokenDto)
                .isOwner(link.isOwner())
                .isAdmin(link.isAdmin())
                .isLocked(link.isLocked())
                .permissions(String.valueOf(link.getPermissions()))
                .createdAt(link.getCreatedAt() != null ? link.getCreatedAt().getEpochSecond() : null)
                .updatedAt(link.getUpdatedAt() != null ? link.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
