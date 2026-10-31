package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.employee.CreateEmployeeRequest;
import com.hashcode.invoicely.dto.user.UserBulkActionRequest;

import java.util.UUID;

public interface EmployeeService {

    GenericManyResponse<UserDto> getEmployees(int page, int perPage);

    ApiResponse<UserDto> getBlankEmployee();

    ApiResponse<UserDto> createEmployee(CreateEmployeeRequest request);

    ApiResponse<UserDto> getEmployee(UUID id);

    ApiResponse<UserDto> updateEmployee(UUID id, CreateEmployeeRequest request);

    ApiResponse<Void> deleteEmployee(UUID id);

    ApiResponse<Void> bulkAction(UserBulkActionRequest request);
}
