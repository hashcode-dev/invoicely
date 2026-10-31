package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.InvoiceItemDto;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.InvoiceItemEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    public InvoiceDto toDto(InvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        List<InvoiceItemDto> itemDtos = entity.getLineItems() != null
                ? entity.getLineItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        return InvoiceDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .recurringId(entity.getRecurringId())
                .number(entity.getNumber())
                .poNumber(entity.getPoNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .partialDueDate(entity.getPartialDueDate() != null ? entity.getPartialDueDate().toString() : null)
                .statusId(entity.getStatusId())
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .paidToDate(entity.getPaidToDate())
                .discount(entity.getDiscount())
                .isAmountDiscount(entity.isAmountDiscount())
                .partial(entity.getPartial())
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
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .exchangeRate(entity.getExchangeRate())
                .designId(entity.getDesignId())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .lineItems(itemDtos)
                .invitations(Collections.emptyList())
                .documents(Collections.emptyList())
                .payments(Collections.emptyList())
                .build();
    }

    public InvoiceItemDto toItemDto(InvoiceItemEntity item) {
        if (item == null) {
            return null;
        }
        return InvoiceItemDto.builder()
                .id(item.getId())
                .productKey(item.getProductKey())
                .notes(item.getNotes())
                .cost(item.getCost())
                .quantity(item.getQuantity())
                .discount(item.getDiscount())
                .isAmountDiscount(item.isAmountDiscount())
                .lineTotal(item.getLineTotal())
                .grossLineTotal(item.getGrossLineTotal())
                .taxName1(item.getTaxName1())
                .taxRate1(item.getTaxRate1())
                .taxName2(item.getTaxName2())
                .taxRate2(item.getTaxRate2())
                .taxName3(item.getTaxName3())
                .taxRate3(item.getTaxRate3())
                .typeId(item.getTypeId())
                .sortId(item.getSortId())
                .customValue1(item.getCustomValue1())
                .customValue2(item.getCustomValue2())
                .customValue3(item.getCustomValue3())
                .customValue4(item.getCustomValue4())
                .build();
    }
}
