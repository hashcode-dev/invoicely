package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.auth.CompanyUserDto;
import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.user.CreateUserRequest;
import com.hashcode.invoicely.dto.user.UserBulkActionRequest;
import com.hashcode.invoicely.entity.relational.AuthProvider;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import com.hashcode.invoicely.mapper.UserMapper;
import com.hashcode.invoicely.repository.relational.CompanyUserRepository;
import com.hashcode.invoicely.repository.relational.UserRepository;
import com.hashcode.invoicely.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CompanyUserRepository companyUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<UserDto> getUsers(int page, int perPage) {
        UUID companyId = TenantContext.getCompanyId();
        List<CompanyUserEntity> links = companyUserRepository.findByCompanyId(companyId);

        List<UserDto> dtoList = new ArrayList<>();
        for (CompanyUserEntity link : links) {
            userRepository.findById(link.getUserId())
                    .ifPresent(u -> dtoList.add(userMapper.toDto(u, link)));
        }

        return GenericManyResponse.of(dtoList, dtoList.size(), page, perPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserDto> getBlankUser() {
        CompanyUserDto blankCompanyUser = CompanyUserDto.builder()
                .isAdmin(false)
                .isOwner(false)
                .isLocked(false)
                .permissions("")
                .notifications(Map.of("email", List.of()))
                .settings(Map.of("accent_color", "#2F7DC3"))
                .build();

        UserDto blankUser = UserDto.builder()
                .organizationId(TenantContext.getAccountId())
                .companyUser(blankCompanyUser)
                .build();

        return ApiResponse.of(blankUser);
    }

    @Override
    @Transactional
    public ApiResponse<UserDto> createUser(CreateUserRequest request) {
        assertOwnerOrAdmin();

        UUID companyId = TenantContext.getCompanyId();
        UUID accountId = TenantContext.getAccountId();

        String email = request.getEmail().trim().toLowerCase();
        Optional<UserEntity> existingUserOpt = userRepository.findByEmailIgnoreCase(email);

        UserEntity user;
        CompanyUserEntity link;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            Optional<CompanyUserEntity> existingLink = companyUserRepository.findByCompanyIdAndUserId(companyId, user.getId());
            if (existingLink.isPresent()) {
                throw new IllegalArgumentException("User with email '" + email + "' is already a team member in this company");
            }

            // Guardrail 1: Reuse existing UserEntity, create distinct CompanyUserEntity for this company
            if (user.getOrganizationId() == null && accountId != null) {
                user.setOrganizationId(accountId);
                user = userRepository.save(user);
            }
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user = userRepository.save(user);
            }

            link = CompanyUserEntity.builder()
                    .companyId(companyId)
                    .userId(user.getId())
                    .accountId(accountId)
                    .isOwner(false)
                    .isAdmin(request.resolveIsAdmin())
                    .permissions(request.resolvePermissions())
                    .build();
            link = companyUserRepository.save(link);
        } else {
            // New user registration
            String rawPassword = request.getPassword() != null && !request.getPassword().isBlank()
                    ? request.getPassword()
                    : UUID.randomUUID().toString();

            user = UserEntity.builder()
                    .email(email)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .organizationId(accountId)
                    .authProvider(AuthProvider.PASSWORD)
                    .isRegistered(true)
                    .build();
            user = userRepository.save(user);

            link = CompanyUserEntity.builder()
                    .companyId(companyId)
                    .userId(user.getId())
                    .accountId(accountId)
                    .isOwner(false)
                    .isAdmin(request.resolveIsAdmin())
                    .permissions(request.resolvePermissions())
                    .build();
            link = companyUserRepository.save(link);
        }

        return ApiResponse.of(userMapper.toDto(user, link));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserDto> getUser(UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        CompanyUserEntity link = companyUserRepository.findByCompanyIdAndUserId(companyId, id).orElse(null);
        return ApiResponse.of(userMapper.toDto(user, link));
    }

    @Override
    @Transactional
    public ApiResponse<UserDto> updateUser(UUID id, CreateUserRequest request) {
        assertOwnerOrAdmin();

        UUID companyId = TenantContext.getCompanyId();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user = userRepository.save(user);

        // Guardrail 2: Scoped updates to CompanyUserEntity for current company only
        Optional<CompanyUserEntity> linkOpt = companyUserRepository.findByCompanyIdAndUserId(companyId, user.getId());
        CompanyUserEntity link = null;
        if (linkOpt.isPresent()) {
            link = linkOpt.get();
            link.setAdmin(request.resolveIsAdmin());
            link.setPermissions(request.resolvePermissions());
            link = companyUserRepository.save(link);
        }

        return ApiResponse.of(userMapper.toDto(user, link));
    }

    @Override
    @Transactional
    public ApiResponse<Void> bulkAction(UserBulkActionRequest request) {
        assertOwnerOrAdmin();

        if (request == null || request.getAction() == null || request.getIds() == null) {
            return ApiResponse.empty();
        }

        UUID companyId = TenantContext.getCompanyId();
        String action = request.getAction().trim().toLowerCase();

        for (UUID userId : request.getIds()) {
            Optional<CompanyUserEntity> linkOpt = companyUserRepository.findByCompanyIdAndUserId(companyId, userId);
            if (linkOpt.isEmpty()) {
                continue;
            }
            CompanyUserEntity link = linkOpt.get();
            switch (action) {
                case "delete":
                    companyUserRepository.delete(link);
                    break;
                case "archive":
                    link.setLocked(true);
                    companyUserRepository.save(link);
                    break;
                case "restore":
                    link.setLocked(false);
                    companyUserRepository.save(link);
                    break;
                default:
                    break;
            }
        }

        return ApiResponse.empty();
    }

    private void assertOwnerOrAdmin() {
        UUID callerId = TenantContext.getUserId();
        UUID companyId = TenantContext.getCompanyId();
        if (callerId == null || companyId == null) {
            return; // If context not yet set (e.g. system seed), allow
        }
        CompanyUserEntity callerLink = companyUserRepository.findByCompanyIdAndUserId(companyId, callerId)
                .orElse(null);
        if (callerLink != null && !callerLink.isOwner() && !callerLink.isAdmin()) {
            throw new AccessDeniedException("Only the account owner or company administrator can manage team members");
        }
    }
}
