package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.payment.CreatePaymentRequest;
import com.hashcode.invoicely.dto.payment.PaymentBulkActionRequest;
import com.hashcode.invoicely.dto.payment.PaymentDto;
import com.hashcode.invoicely.dto.payment.PaymentRefundRequest;
import com.hashcode.invoicely.dto.payment.PaymentableDto;
import com.hashcode.invoicely.entity.relational.PaymentEntity;
import com.hashcode.invoicely.entity.relational.PaymentableEntity;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    GenericManyResponse<PaymentDto> getPayments(int page, int perPage, String status, UUID clientId, UUID invoiceId, String filter, String sort);

    PaymentDto getBlankPayment(UUID clientId);

    PaymentDto getPaymentById(UUID id);

    PaymentDto createPayment(CreatePaymentRequest request);

    PaymentDto refundPayment(UUID id, PaymentRefundRequest request);

    List<PaymentDto> bulkAction(PaymentBulkActionRequest request);

    PaymentDto toDto(PaymentEntity entity);

    PaymentableDto toPaymentableDto(PaymentableEntity entity);
}
