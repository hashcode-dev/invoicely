package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.entity.relational.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(UserEntity user) {
        if (user == null) {
            return null;
        }
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
