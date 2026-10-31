package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.auth.CompanyUserDto;
import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.entity.relational.CompanyUserEntity;
import com.hashcode.invoicely.entity.relational.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(UserEntity user) {
        return toDto(user, null);
    }

    public UserDto toDto(UserEntity user, CompanyUserEntity link) {
        if (user == null) {
            return null;
        }

        CompanyUserDto companyUserDto = null;
        if (link != null) {
            companyUserDto = CompanyUserDto.builder()
                    .id(link.getId())
                    .companyId(link.getCompanyId())
                    .userId(link.getUserId())
                    .accountId(link.getAccountId())
                    .isOwner(link.isOwner())
                    .isAdmin(link.isAdmin())
                    .isLocked(link.isLocked())
                    .permissions(String.valueOf(link.getPermissions()))
                    .createdAt(link.getCreatedAt() != null ? link.getCreatedAt().getEpochSecond() : null)
                    .updatedAt(link.getUpdatedAt() != null ? link.getUpdatedAt().getEpochSecond() : null)
                    .build();
        }

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatar(user.getAvatarUrl())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .organizationId(user.getOrganizationId() != null ? user.getOrganizationId() : (link != null ? link.getAccountId() : null))
                .companyUser(companyUserDto)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().getEpochSecond() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
