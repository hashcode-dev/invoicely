package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.user.CreateUserRequest;

import java.util.UUID;

public interface UserService {

    GenericManyResponse<UserDto> getUsers(int page, int perPage);

    ApiResponse<UserDto> createUser(CreateUserRequest request);

    ApiResponse<UserDto> getUser(UUID id);
}
