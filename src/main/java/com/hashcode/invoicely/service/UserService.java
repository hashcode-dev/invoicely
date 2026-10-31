package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.user.CreateUserRequest;
import com.hashcode.invoicely.dto.user.UserBulkActionRequest;

import java.util.UUID;

public interface UserService {

    GenericManyResponse<UserDto> getUsers(int page, int perPage);

    ApiResponse<UserDto> getBlankUser();

    ApiResponse<UserDto> createUser(CreateUserRequest request);

    ApiResponse<UserDto> getUser(UUID id);

    ApiResponse<UserDto> updateUser(UUID id, CreateUserRequest request);

    ApiResponse<Void> bulkAction(UserBulkActionRequest request);
}
