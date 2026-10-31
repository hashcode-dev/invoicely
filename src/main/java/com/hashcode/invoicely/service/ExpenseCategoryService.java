package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.entity.relational.ExpenseCategoryEntity;

import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryService {

    GenericManyResponse<ExpenseCategoryDto> getExpenseCategories(int page, int perPage, String status, String filter, String sort);

    ExpenseCategoryDto getExpenseCategoryById(UUID id);

    ExpenseCategoryDto createExpenseCategory(ExpenseCategoryDto dto);

    ExpenseCategoryDto updateExpenseCategory(UUID id, ExpenseCategoryDto dto);

    void deleteExpenseCategory(UUID id);

    List<ExpenseCategoryDto> bulkAction(ExpenseCategoryBulkActionRequest request);

    ExpenseCategoryDto toDto(ExpenseCategoryEntity entity);
}
