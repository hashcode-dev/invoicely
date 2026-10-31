package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.procurement.ExpenseDto;
import com.hashcode.invoicely.entity.relational.ExpenseEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.ExpenseCategoryRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import com.hashcode.invoicely.repository.relational.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ExpenseMapper {

    private final VendorRepository vendorRepository;
    private final ClientRepository clientRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorMapper vendorMapper;
    private final ClientMapper clientMapper;
    private final ExpenseCategoryMapper expenseCategoryMapper;
    private final InvoiceMapper invoiceMapper;

    public ExpenseDto toDto(ExpenseEntity entity) {
        if (entity == null) {
            return null;
        }
        return ExpenseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .vendorId(entity.getVendorId())
                .clientId(entity.getClientId())
                .invoiceId(entity.getInvoiceId())
                .categoryId(entity.getCategoryId())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .number(entity.getNumber())
                .amount(entity.getAmount())
                .foreignAmount(entity.getForeignAmount())
                .exchangeRate(entity.getExchangeRate())
                .currencyId(entity.getCurrencyId())
                .taxName1(entity.getTaxName1())
                .taxRate1(entity.getTaxRate1())
                .taxName2(entity.getTaxName2())
                .taxRate2(entity.getTaxRate2())
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .taxAmount1(entity.getTaxAmount1())
                .taxAmount2(entity.getTaxAmount2())
                .taxAmount3(entity.getTaxAmount3())
                .usesInclusiveTaxes(entity.isUsesInclusiveTaxes())
                .shouldBeInvoiced(entity.isShouldBeInvoiced())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .paymentDate(entity.getPaymentDate() != null ? entity.getPaymentDate().toString() : null)
                .paymentTypeId(entity.getPaymentTypeId())
                .transactionReference(entity.getTransactionReference())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .statusId(entity.getStatusId())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .vendor(entity.getVendorId() != null ? vendorRepository.findById(entity.getVendorId()).map(vendorMapper::toDto).orElse(null) : null)
                .client(entity.getClientId() != null ? clientRepository.findById(entity.getClientId()).map(clientMapper::toDto).orElse(null) : null)
                .category(entity.getCategoryId() != null ? expenseCategoryRepository.findById(entity.getCategoryId()).map(expenseCategoryMapper::toDto).orElse(null) : null)
                .invoice(entity.getInvoiceId() != null ? invoiceRepository.findById(entity.getInvoiceId()).map(invoiceMapper::toDto).orElse(null) : null)
                .documents(Collections.emptyList())
                .build();
    }
}
