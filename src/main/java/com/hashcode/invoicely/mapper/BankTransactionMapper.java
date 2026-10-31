package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.banking.BankTransactionDto;
import com.hashcode.invoicely.entity.relational.BankTransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class BankTransactionMapper {

    public BankTransactionDto toDto(BankTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return BankTransactionDto.builder()
                .id(entity.getId())
                .transactionId(entity.getId())
                .userId(entity.getUserId())
                .bankIntegrationId(entity.getBankIntegrationId())
                .bankTransactionRuleId(entity.getBankTransactionRuleId())
                .date(entity.getDate())
                .amount(entity.getAmount())
                .currencyId(entity.getCurrencyId())
                .description(entity.getDescription())
                .baseType(entity.getBaseType())
                .statusId(entity.getStatusId())
                .paymentId(entity.getPaymentId())
                .expenseId(entity.getExpenseId())
                .vendorId(entity.getVendorId())
                .categoryId(entity.getCategoryId())
                .invoicelyCategoryId(entity.getCategoryId())
                .participant(entity.getParticipant())
                .participantName(entity.getParticipantName())
                .invoiceIds(entity.getInvoiceIds())
                .accountType(entity.getAccountType())
                .categoryType(entity.getCategoryType())
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
