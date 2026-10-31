package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.invoice.CreditDto;
import com.hashcode.invoicely.entity.relational.CreditEntity;
import org.springframework.stereotype.Component;

@Component
public class CreditMapper {

    public CreditDto toDto(CreditEntity entity) {
        if (entity == null) {
            return null;
        }

        return CreditDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .clientId(entity.getClientId())
                .invoiceId(entity.getInvoiceId())
                .number(entity.getNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .statusId(entity.getStatusId())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .terms(entity.getTerms())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
