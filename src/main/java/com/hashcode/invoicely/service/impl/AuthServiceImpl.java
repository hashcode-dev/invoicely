package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.AccountDto;
import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.dto.auth.CompanyUserResponse;
import com.hashcode.invoicely.dto.auth.LoginPrecheckRequest;
import com.hashcode.invoicely.dto.auth.LoginPrecheckResponse;
import com.hashcode.invoicely.dto.auth.LoginRequest;
import com.hashcode.invoicely.dto.auth.TokenDto;
import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.entity.relational.AccountEntity;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import com.hashcode.invoicely.mapper.AuthMapper;
import com.hashcode.invoicely.repository.relational.AccountRepository;
import com.hashcode.invoicely.repository.relational.ApiTokenRepository;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.repository.relational.UserRepository;
import com.hashcode.invoicely.service.AuthService;
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
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;

    @Override
    @Transactional(readOnly = true)
    public LoginPrecheckResponse precheck(LoginPrecheckRequest request) {
        return LoginPrecheckResponse.builder()
                .methods(List.of("password"))
                .secretRequired(false)
                .build();
    }

    @Override
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

    @Override
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
        UserDto userDto = authMapper.toUserDto(user);

        for (CompanyUserEntity link : links) {
            Optional<CompanyEntity> companyOpt = companyRepository.findById(link.getCompanyId());
            if (companyOpt.isEmpty() || companyOpt.get().isDeleted()) {
                continue;
            }
            CompanyEntity company = companyOpt.get();

            AccountEntity account = accountRepository.findById(link.getAccountId())
                    .orElseGet(() -> AccountEntity.builder().id(link.getAccountId()).defaultCompanyId(company.getId()).build());

            String tokenString = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), company.getId(), account.getId());
            TokenDto tokenDto = authMapper.toTokenDto(UUID.randomUUID(), tokenString, user.getId());
            CompanyDto companyDto = authMapper.toCompanyDto(company);
            AccountDto accountDto = authMapper.toAccountDto(account, company.getId());

            CompanyUserResponse response = authMapper.toCompanyUserResponse(userDto, companyDto, accountDto, tokenDto, link);
            responses.add(response);
        }

        return responses;
    }
}
