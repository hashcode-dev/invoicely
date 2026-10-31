package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.banking.BankAccountDto;
import com.hashcode.invoicely.dto.banking.BankIntegrationBulkActionRequest;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.BankIntegrationEntity;

import java.util.List;
import java.util.UUID;

public interface BankIntegrationService {

    GenericManyResponse<BankAccountDto> getBankIntegrations(int page, int perPage, String status, String filter, String sort);

    BankAccountDto getBankIntegrationById(UUID id);

    BankAccountDto getBlankBankIntegration();

    BankAccountDto createBankIntegration(BankAccountDto dto);

    BankAccountDto updateBankIntegration(UUID id, BankAccountDto dto);

    void deleteBankIntegration(UUID id);

    int handleBulkAction(BankIntegrationBulkActionRequest request);

    List<BankAccountDto> refreshAccounts();

    BankAccountDto toDto(BankIntegrationEntity entity);
}
