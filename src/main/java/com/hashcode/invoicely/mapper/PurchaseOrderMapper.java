package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.procurement.PurchaseOrderDto;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderItemDto;
import com.hashcode.invoicely.entity.relational.PurchaseOrderEntity;
import com.hashcode.invoicely.entity.relational.PurchaseOrderItemEntity;
import com.hashcode.invoicely.repository.relational.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PurchaseOrderMapper {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    public PurchaseOrderDto toDto(PurchaseOrderEntity entity) {
        if (entity == null) {
            return null;
        }

        List<PurchaseOrderItemDto> items = entity.getLineItems() != null
                ? entity.getLineItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        return PurchaseOrderDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .vendorId(entity.getVendorId())
                .clientId(entity.getClientId())
                .number(entity.getNumber())
                .poNumber(entity.getPoNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .statusId(entity.getStatusId())
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .paidToDate(entity.getPaidToDate())
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
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .exchangeRate(entity.getExchangeRate())
                .designId(entity.getDesignId())
                .expenseId(entity.getExpenseId())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .lineItems(items)
                .vendor(vendorRepository.findById(entity.getVendorId()).map(vendorMapper::toDto).orElse(null))
                .documents(Collections.emptyList())
                .build();
    }

    public PurchaseOrderItemDto toItemDto(PurchaseOrderItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return PurchaseOrderItemDto.builder()
                .id(entity.getId())
                .productKey(entity.getProductKey())
                .notes(entity.getNotes())
                .cost(entity.getCost())
                .quantity(entity.getQuantity())
                .discount(entity.getDiscount())
                .isAmountDiscount(entity.isAmountDiscount())
                .taxName1(entity.getTaxName1())
                .taxRate1(entity.getTaxRate1())
                .taxName2(entity.getTaxName2())
                .taxRate2(entity.getTaxRate2())
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .lineTotal(entity.getLineTotal())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
