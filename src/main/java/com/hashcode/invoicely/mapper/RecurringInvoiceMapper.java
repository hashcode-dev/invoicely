package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.invoice.RecurringInvoiceDto;
import com.hashcode.invoicely.entity.relational.RecurringInvoiceEntity;
import org.springframework.stereotype.Component;

@Component
public class RecurringInvoiceMapper {

    public RecurringInvoiceDto toDto(RecurringInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        return RecurringInvoiceDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .clientId(entity.getClientId())
                .number(entity.getNumber())
                .frequencyId(entity.getFrequencyId())
                .statusId(entity.getStatusId())
                .autoBill(entity.isAutoBill())
                .amount(entity.getAmount())
                .remainingCycles(entity.getRemainingCycles())
                .nextSendDate(entity.getNextSendDate() != null ? entity.getNextSendDate().toString() : null)
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
