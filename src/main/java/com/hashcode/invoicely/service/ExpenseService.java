package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseDto;
import com.hashcode.invoicely.entity.relational.ExpenseEntity;

import java.util.List;
import java.util.UUID;

public interface ExpenseService {

    GenericManyResponse<ExpenseDto> getExpenses(int page, int perPage, String status, UUID vendorId, UUID clientId, String filter, String sort);

    ExpenseDto getBlankExpense();

    ExpenseDto getExpenseById(UUID id);

    ExpenseDto createExpense(ExpenseDto dto);

    ExpenseDto updateExpense(UUID id, ExpenseDto dto);

    void deleteExpense(UUID id);

    List<ExpenseDto> bulkAction(ExpenseBulkActionRequest request);

    ExpenseDto toDto(ExpenseEntity entity);
}
