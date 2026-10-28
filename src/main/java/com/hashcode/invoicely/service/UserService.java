package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.user.CreateUserRequest;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.repository.relational.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyUserRepository companyUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public GenericManyResponse<UserDto> getUsers(int page, int perPage) {
        UUID companyId = TenantContext.getCompanyId();
        List<CompanyUserEntity> links = companyUserRepository.findByCompanyId(companyId);

        List<UserDto> dtoList = new ArrayList<>();
        for (CompanyUserEntity link : links) {
            userRepository.findById(link.getUserId())
                    .ifPresent(u -> dtoList.add(toDto(u)));
        }

        return GenericManyResponse.of(dtoList, dtoList.size(), page, perPage);
    }

    @Transactional
    public ApiResponse<UserDto> createUser(CreateUserRequest request) {
        UUID companyId = TenantContext.getCompanyId();
        UUID accountId = TenantContext.getAccountId();

        String email = request.getEmail().trim().toLowerCase();
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    String passwordHash = request.getPassword() != null && !request.getPassword().isBlank()
                            ? passwordEncoder.encode(request.getPassword())
                            : passwordEncoder.encode(UUID.randomUUID().toString());

                    UserEntity newUser = UserEntity.builder()
                            .email(email)
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phone(request.getPhone())
                            .passwordHash(passwordHash)
                            .isRegistered(true)
                            .build();
                    return userRepository.save(newUser);
                });

        Optional<CompanyUserEntity> existingLink = companyUserRepository.findByCompanyIdAndUserId(companyId, user.getId());
        if (existingLink.isEmpty()) {
            CompanyUserEntity link = CompanyUserEntity.builder()
                    .companyId(companyId)
                    .userId(user.getId())
                    .accountId(accountId)
                    .isAdmin(request.isAdmin())
                    .permissions(request.getPermissions() != null ? request.getPermissions() : 0L)
                    .build();
            companyUserRepository.save(link);
        }

        return ApiResponse.of(toDto(user));
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserDto> getUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ApiResponse.of(toDto(user));
    }

    private UserDto toDto(UserEntity user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().getEpochSecond() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
