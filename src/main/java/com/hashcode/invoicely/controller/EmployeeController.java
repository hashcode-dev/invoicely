package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.employee.CreateEmployeeRequest;
import com.hashcode.invoicely.dto.user.UserBulkActionRequest;
import com.hashcode.invoicely.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<UserDto>> getEmployees(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage) {
        return ResponseEntity.ok(employeeService.getEmployees(page, perPage));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<UserDto>> getBlankEmployee() {
        return ResponseEntity.ok(employeeService.getBlankEmployee());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getEmployee(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateEmployee(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(employeeService.deleteEmployee(id));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkAction(@RequestBody UserBulkActionRequest request) {
        return ResponseEntity.ok(employeeService.bulkAction(request));
    }
}
