package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.banking.BankTransactionBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionDto;
import com.hashcode.invoicely.dto.banking.BankTransactionMatchRequest;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.BankTransactionEntity;

import java.util.Map;
import java.util.UUID;

public interface BankTransactionService {

    GenericManyResponse<BankTransactionDto> getTransactions(int page, int perPage, String status, UUID bankIntegrationId, String baseType, String filter, String sort);

    BankTransactionDto getTransactionById(UUID id);

    BankTransactionDto getBlankTransaction();

    BankTransactionDto createTransaction(BankTransactionDto dto);

    BankTransactionDto updateTransaction(UUID id, BankTransactionDto dto);

    void deleteTransaction(UUID id);

    Map<String, Object> handleBulkAction(BankTransactionBulkActionRequest request);

    Map<String, Object> matchTransactions(BankTransactionMatchRequest request);

    BankTransactionDto toDto(BankTransactionEntity entity);
}
