package com.hashcode.invoicely.service;

import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.*;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginPrecheckResponse precheck(LoginPrecheckRequest request) {
        return LoginPrecheckResponse.builder()
                .methods(List.of("password"))
                .secretRequired(false)
                .build();
    }

    @Transactional
    public ApiResponse<List<CompanyUserResponse>> login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        List<CompanyUserEntity> companyUserLinks = companyUserRepository.findByUserId(user.getId());

        if (companyUserLinks.isEmpty()) {
            companyUserLinks = List.of(provisionDefaultAccountAndCompany(user));
        }

        List<CompanyUserResponse> responses = buildCompanyUserResponses(user, companyUserLinks);
        return ApiResponse.of(responses);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<CompanyUserResponse>> refresh(UUID currentCompanyId) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new BadCredentialsException("Unauthenticated session");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        List<CompanyUserEntity> companyUserLinks = companyUserRepository.findByUserId(user.getId());
        if (companyUserLinks.isEmpty()) {
            companyUserLinks = List.of(provisionDefaultAccountAndCompany(user));
        }

        List<CompanyUserResponse> responses = buildCompanyUserResponses(user, companyUserLinks);
        return ApiResponse.of(responses);
    }

    private CompanyUserEntity provisionDefaultAccountAndCompany(UserEntity user) {
        AccountEntity account = AccountEntity.builder()
                .plan("enterprise")
                .build();
        account = accountRepository.save(account);

        String companyName = (user.getFirstName() != null ? user.getFirstName() : "Default") + "'s Company";
        String subdomain = "company-" + UUID.randomUUID().toString().substring(0, 8);

        CompanyEntity company = CompanyEntity.builder()
                .accountId(account.getId())
                .companyName(companyName)
                .subdomain(subdomain)
                .currencyId("USD")
                .countryId("US")
                .build();
        company = companyRepository.save(company);

        account.setDefaultCompanyId(company.getId());
        accountRepository.save(account);

        CompanyUserEntity link = CompanyUserEntity.builder()
                .companyId(company.getId())
                .userId(user.getId())
                .accountId(account.getId())
                .isOwner(true)
                .isAdmin(true)
                .permissions(-1L)
                .build();
        return companyUserRepository.save(link);
    }

    private List<CompanyUserResponse> buildCompanyUserResponses(UserEntity user, List<CompanyUserEntity> links) {
        List<CompanyUserResponse> responses = new ArrayList<>();

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().getEpochSecond() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().getEpochSecond() : null)
                .build();

        for (CompanyUserEntity link : links) {
            Optional<CompanyEntity> companyOpt = companyRepository.findById(link.getCompanyId());
            if (companyOpt.isEmpty() || companyOpt.get().isDeleted()) {
                continue;
            }
            CompanyEntity company = companyOpt.get();

            AccountEntity account = accountRepository.findById(link.getAccountId())
                    .orElseGet(() -> AccountEntity.builder().id(link.getAccountId()).defaultCompanyId(company.getId()).build());

            String tokenString = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), company.getId(), account.getId());

            TokenDto tokenDto = TokenDto.builder()
                    .id(UUID.randomUUID())
                    .token(tokenString)
                    .name("Web Session")
                    .userId(user.getId())
                    .createdAt(System.currentTimeMillis() / 1000)
                    .updatedAt(System.currentTimeMillis() / 1000)
                    .build();

            CompanyDto companyDto = CompanyDto.builder()
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
                    .createdAt(company.getCreatedAt() != null ? company.getCreatedAt().getEpochSecond() : null)
                    .updatedAt(company.getUpdatedAt() != null ? company.getUpdatedAt().getEpochSecond() : null)
                    .build();

            AccountDto accountDto = AccountDto.builder()
                    .id(account.getId())
                    .defaultCompanyId(account.getDefaultCompanyId() != null ? account.getDefaultCompanyId() : company.getId())
                    .plan(account.getPlan())
                    .createdAt(account.getCreatedAt() != null ? account.getCreatedAt().getEpochSecond() : null)
                    .updatedAt(account.getUpdatedAt() != null ? account.getUpdatedAt().getEpochSecond() : null)
                    .build();

            CompanyUserResponse response = CompanyUserResponse.builder()
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

            responses.add(response);
        }

        return responses;
    }
}
