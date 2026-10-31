package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.invoice.QuoteDto;
import com.hashcode.invoicely.dto.invoice.QuoteItemDto;
import com.hashcode.invoicely.entity.relational.QuoteEntity;
import com.hashcode.invoicely.entity.relational.QuoteItemEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuoteMapper {

    public QuoteDto toDto(QuoteEntity entity) {
        if (entity == null) {
            return null;
        }

        List<QuoteItemDto> itemDtos = entity.getLineItems() != null
                ? entity.getLineItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        return QuoteDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .invoiceId(entity.getInvoiceId())
                .number(entity.getNumber())
                .poNumber(entity.getPoNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .statusId(entity.getStatusId())
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .discount(entity.getDiscount())
                .isAmountDiscount(entity.isAmountDiscount())
                .totalTaxes(entity.getTotalTaxes())
                .terms(entity.getTerms())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .footer(entity.getFooter())
                .usesInclusiveTaxes(entity.isUsesInclusiveTaxes())
                .taxName1(entity.getTaxName1())
                .taxRate1(entity.getTaxRate1())
                .taxName2(entity.getTaxName2())
                .taxRate2(entity.getTaxRate2())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .lineItems(itemDtos)
                .invitations(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
    }

    public QuoteItemDto toItemDto(QuoteItemEntity item) {
        if (item == null) {
            return null;
        }
        return QuoteItemDto.builder()
                .id(item.getId())
                .productKey(item.getProductKey())
                .notes(item.getNotes())
                .cost(item.getCost())
                .quantity(item.getQuantity())
                .discount(item.getDiscount())
                .isAmountDiscount(item.isAmountDiscount())
                .lineTotal(item.getLineTotal())
                .taxName1(item.getTaxName1())
                .taxRate1(item.getTaxRate1())
                .sortId(item.getSortId())
                .build();
    }
}
