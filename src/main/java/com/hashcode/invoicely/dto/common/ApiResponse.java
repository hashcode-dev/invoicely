package com.hashcode.invoicely.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard single-resource wrapper matching Invoicely Front-Desk API contract:
 * { "data": T }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private T data;

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }

    public static <T> ApiResponse<T> empty() {
        return new ApiResponse<>(null);
    }
}
