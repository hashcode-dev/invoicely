package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.auth.UserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.user.CreateUserRequest;
import com.hashcode.invoicely.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<UserDto>> getUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage) {
        return ResponseEntity.ok(userService.getUsers(page, perPage));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }
}
