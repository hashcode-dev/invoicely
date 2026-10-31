package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.banking.BankTransactionRuleBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleDto;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.BankTransactionEntity;
import com.hashcode.invoicely.entity.relational.BankTransactionRuleEntity;

import java.util.UUID;

public interface BankTransactionRuleService {

    GenericManyResponse<BankTransactionRuleDto> getRules(int page, int perPage, String status, String filter, String sort);

    BankTransactionRuleDto getRuleById(UUID id);

    BankTransactionRuleDto getBlankRule();

    BankTransactionRuleDto createRule(BankTransactionRuleDto dto);

    BankTransactionRuleDto updateRule(UUID id, BankTransactionRuleDto dto);

    void deleteRule(UUID id);

    int handleBulkAction(BankTransactionRuleBulkActionRequest request);

    boolean matches(BankTransactionRuleEntity rule, BankTransactionEntity transaction);

    BankTransactionRuleDto toDto(BankTransactionRuleEntity entity);
}
