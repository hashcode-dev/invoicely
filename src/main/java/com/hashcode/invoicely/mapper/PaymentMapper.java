package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.payment.PaymentDto;
import com.hashcode.invoicely.dto.payment.PaymentableDto;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.PaymentEntity;
import com.hashcode.invoicely.entity.relational.PaymentableEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceMapper invoiceMapper;
    private final ClientMapper clientMapper;

    public PaymentDto toDto(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        List<PaymentableDto> paymentableDtos = entity.getPaymentables() != null
                ? entity.getPaymentables().stream().map(this::toPaymentableDto).collect(Collectors.toList())
                : Collections.emptyList();

        List<InvoiceDto> invoiceDtos = new ArrayList<>();
        if (entity.getPaymentables() != null) {
            for (PaymentableEntity p : entity.getPaymentables()) {
                invoiceRepository.findById(p.getInvoiceId()).ifPresent(inv -> invoiceDtos.add(invoiceMapper.toDto(inv)));
            }
        }

        return PaymentDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .companyGatewayId(entity.getCompanyGatewayId())
                .number(entity.getNumber())
                .amount(entity.getAmount())
                .refunded(entity.getRefunded())
                .applied(entity.getApplied())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .typeId(entity.getTypeId())
                .transactionReference(entity.getTransactionReference())
                .privateNotes(entity.getPrivateNotes())
                .statusId(entity.getStatusId())
                .isManual(entity.isManual())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .paymentables(paymentableDtos)
                .invoices(invoiceDtos)
                .client(clientRepository.findById(entity.getClientId()).map(clientMapper::toDto).orElse(null))
                .documents(Collections.emptyList())
                .build();
    }

    public PaymentableDto toPaymentableDto(PaymentableEntity entity) {
        if (entity == null) {
            return null;
        }
        String invNumber = invoiceRepository.findById(entity.getInvoiceId())
                .map(InvoiceEntity::getNumber)
                .orElse(null);

        return PaymentableDto.builder()
                .id(entity.getId())
                .paymentId(entity.getPayment() != null ? entity.getPayment().getId() : null)
                .invoiceId(entity.getInvoiceId())
                .invoiceNumber(invNumber)
                .amount(entity.getAmount())
                .refunded(entity.getRefunded())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
