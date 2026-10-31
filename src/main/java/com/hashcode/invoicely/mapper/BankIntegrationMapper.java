package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.banking.BankAccountDto;
import com.hashcode.invoicely.entity.relational.BankIntegrationEntity;
import org.springframework.stereotype.Component;

@Component
public class BankIntegrationMapper {

    public BankAccountDto toDto(BankIntegrationEntity entity) {
        if (entity == null) {
            return null;
        }
        return BankAccountDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .bankAccountName(entity.getBankAccountName())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankAccountType(entity.getBankAccountType())
                .bankAccountStatus(entity.getBankAccountStatus())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .nickname(entity.getNickname())
                .providerName(entity.getProviderName())
                .integrationType(entity.getIntegrationType())
                .nordigenInstitutionId(entity.getNordigenInstitutionId())
                .autoSync(Boolean.TRUE.equals(entity.getAutoSync()))
                .disabledUpstream(Boolean.TRUE.equals(entity.getDisabledUpstream()))
                .fromDate(entity.getFromDate())
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
