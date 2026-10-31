package com.hashcode.invoicely.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.AccountDto;
import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.dto.auth.CompanyUserResponse;
import com.hashcode.invoicely.dto.auth.EmployeeLoginRequest;
import com.hashcode.invoicely.dto.auth.GoogleLoginRequest;
import com.hashcode.invoicely.dto.auth.LoginPrecheckRequest;
import com.hashcode.invoicely.dto.auth.LoginPrecheckResponse;
import com.hashcode.invoicely.dto.auth.LoginRequest;
import com.hashcode.invoicely.dto.auth.RegisterRequest;
import com.hashcode.invoicely.dto.auth.TokenDto;
import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.entity.relational.AccountEntity;
import com.hashcode.invoicely.entity.relational.AuthProvider;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import com.hashcode.invoicely.exception.AccountProviderMismatchException;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public LoginPrecheckResponse precheck(LoginPrecheckRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return LoginPrecheckResponse.builder()
                    .methods(List.of("password", "google"))
                    .secretRequired(false)
                    .build();
        }

        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(request.getEmail().trim());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getAuthProvider() == AuthProvider.GOOGLE) {
                return LoginPrecheckResponse.builder()
                        .methods(List.of("google"))
                        .secretRequired(false)
                        .authProvider("GOOGLE")
                        .build();
            } else {
                return LoginPrecheckResponse.builder()
                        .methods(List.of("password"))
                        .secretRequired(false)
                        .authProvider("PASSWORD")
                        .build();
            }
        }

        return LoginPrecheckResponse.builder()
                .methods(List.of("password", "google"))
                .secretRequired(false)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<CompanyUserResponse>> login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new AccountProviderMismatchException(
                    "An account with this email is already registered using Google Sign-In. Please use the 'Sign in with Google' option.",
                    "GOOGLE");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        List<CompanyUserEntity> companyUserLinks = companyUserRepository.findByUserId(user.getId());

        if (companyUserLinks.isEmpty()) {
            companyUserLinks = List.of(provisionDefaultAccountAndCompany(user, null));
        }

        List<CompanyUserResponse> responses = buildCompanyUserResponses(user, companyUserLinks);
        return ApiResponse.of(responses);
    }

    @Override
    @Transactional
    public ApiResponse<List<CompanyUserResponse>> register(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<UserEntity> existingUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUserOpt.isPresent()) {
            UserEntity existingUser = existingUserOpt.get();
            if (existingUser.getAuthProvider() == AuthProvider.GOOGLE) {
                throw new AccountProviderMismatchException(
                        "An account with this email is already registered using Google Sign-In. Please use the 'Sign in with Google' option.",
                        "GOOGLE");
            } else {
                throw new AccountProviderMismatchException(
                        "An account with this email is already registered. Please sign in with your email and password.",
                        "PASSWORD");
            }
        }

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .authProvider(AuthProvider.PASSWORD)
                .isRegistered(true)
                .build();
        user = userRepository.save(user);

        CompanyUserEntity link = provisionDefaultAccountAndCompany(user, request.getCompanyName());
        List<CompanyUserResponse> responses = buildCompanyUserResponses(user, List.of(link));
        return ApiResponse.of(responses);
    }

    @Override
    @Transactional
    public ApiResponse<List<CompanyUserResponse>> loginWithGoogle(GoogleLoginRequest request) {
        if (request == null) {
            throw new BadCredentialsException("Google authentication request cannot be empty");
        }

        String email = request.getEmail();
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        String avatarUrl = request.getAvatarUrl();
        String sub = request.getSub();

        if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
            try {
                String token = request.getIdToken().trim();
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                    JsonNode claims = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
                    if (claims.hasNonNull("email")) {
                        email = claims.get("email").asText();
                    }
                    if (claims.hasNonNull("given_name")) {
                        firstName = claims.get("given_name").asText();
                    } else if (claims.hasNonNull("first_name")) {
                        firstName = claims.get("first_name").asText();
                    }
                    if (claims.hasNonNull("family_name")) {
                        lastName = claims.get("family_name").asText();
                    } else if (claims.hasNonNull("last_name")) {
                        lastName = claims.get("last_name").asText();
                    }
                    if (claims.hasNonNull("picture")) {
                        avatarUrl = claims.get("picture").asText();
                    } else if (claims.hasNonNull("avatar_url")) {
                        avatarUrl = claims.get("avatar_url").asText();
                    }
                    if (claims.hasNonNull("sub")) {
                        sub = claims.get("sub").asText();
                    }
                }
            } catch (Exception e) {
                if (email == null || email.isBlank()) {
                    throw new BadCredentialsException("Failed to parse Google ID token claims: " + e.getMessage());
                }
            }
        }

        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Email is required for Google Sign-In");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<UserEntity> existingUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        UserEntity user;
        List<CompanyUserEntity> companyUserLinks;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getAuthProvider() != null && user.getAuthProvider() != AuthProvider.GOOGLE) {
                throw new AccountProviderMismatchException(
                        "This account is registered using email and password. Please sign in using your email and password.",
                        "PASSWORD");
            }

            boolean updated = false;
            if (firstName != null && !firstName.isBlank() && user.getFirstName() == null) {
                user.setFirstName(firstName);
                updated = true;
            }
            if (lastName != null && !lastName.isBlank() && user.getLastName() == null) {
                user.setLastName(lastName);
                updated = true;
            }
            if (avatarUrl != null && !avatarUrl.isBlank() && user.getAvatarUrl() == null) {
                user.setAvatarUrl(avatarUrl);
                updated = true;
            }
            if (sub != null && !sub.isBlank() && user.getOauthId() == null) {
                user.setOauthId(sub);
                updated = true;
            }
            if (user.getAuthProvider() == null) {
                user.setAuthProvider(AuthProvider.GOOGLE);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }

            companyUserLinks = companyUserRepository.findByUserId(user.getId());
            if (companyUserLinks.isEmpty()) {
                companyUserLinks = List.of(provisionDefaultAccountAndCompany(user, null));
            }
        } else {
            user = UserEntity.builder()
                    .email(normalizedEmail)
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(avatarUrl)
                    .oauthId(sub)
                    .authProvider(AuthProvider.GOOGLE)
                    .isRegistered(true)
                    .build();
            user = userRepository.save(user);
            companyUserLinks = List.of(provisionDefaultAccountAndCompany(user, null));
        }

        List<CompanyUserResponse> responses = buildCompanyUserResponses(user, companyUserLinks);
        return ApiResponse.of(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CompanyUserResponse>> employeeLogin(EmployeeLoginRequest request) {
        if (request == null || request.getOrganizationId() == null || request.getEmployeeId() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Organization ID, Employee ID, and Password are required");
        }

        UUID organizationId;
        try {
            organizationId = UUID.fromString(request.getOrganizationId().trim());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid Organization ID format");
        }

        AccountEntity account = accountRepository.findById(organizationId)
                .orElseThrow(() -> new BadCredentialsException("Organization not found"));

        String employeeIdentifier = request.getEmployeeId().trim();
        Optional<UserEntity> userOpt;
        try {
            UUID employeeUuid = UUID.fromString(employeeIdentifier);
            userOpt = userRepository.findById(employeeUuid);
        } catch (IllegalArgumentException e) {
            userOpt = userRepository.findByEmailIgnoreCase(employeeIdentifier);
        }

        UserEntity employee = userOpt
                .orElseThrow(() -> new BadCredentialsException("Invalid employee ID or password"));

        if (employee.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            throw new BadCredentialsException("Invalid employee ID or password");
        }

        List<CompanyUserEntity> allLinks = companyUserRepository.findByUserId(employee.getId());
        List<CompanyUserEntity> organizationLinks = allLinks.stream()
                .filter(link -> link.getAccountId().equals(account.getId()))
                .toList();

        if (organizationLinks.isEmpty()) {
            throw new BadCredentialsException("Employee is not assigned to any active company workspace within this organization");
        }

        List<CompanyUserResponse> responses = buildCompanyUserResponses(employee, organizationLinks);
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
            companyUserLinks = List.of(provisionDefaultAccountAndCompany(user, null));
        }

        List<CompanyUserResponse> responses = buildCompanyUserResponses(user, companyUserLinks);
        return ApiResponse.of(responses);
    }

    private CompanyUserEntity provisionDefaultAccountAndCompany(UserEntity user, String customCompanyName) {
        AccountEntity account = AccountEntity.builder()
                .plan("enterprise")
                .build();
        account = accountRepository.save(account);

        if (user.getOrganizationId() == null) {
            user.setOrganizationId(account.getId());
            userRepository.save(user);
        }

        String companyName;
        if (customCompanyName != null && !customCompanyName.isBlank()) {
            companyName = customCompanyName.trim();
        } else {
            companyName = (user.getFirstName() != null && !user.getFirstName().isBlank()
                    ? user.getFirstName() : "Default") + "'s Company";
        }
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
