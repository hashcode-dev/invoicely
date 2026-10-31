package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.RecurringInvoiceDto;
import com.hashcode.invoicely.entity.relational.RecurringInvoiceEntity;

import java.util.UUID;

public interface RecurringInvoiceService {

    GenericManyResponse<RecurringInvoiceDto> getRecurringInvoices(int page, int perPage, String status, String filter);

    RecurringInvoiceDto getBlankRecurringInvoice();

    RecurringInvoiceDto getRecurringInvoiceById(UUID id);

    RecurringInvoiceDto createRecurringInvoice(RecurringInvoiceDto dto);

    RecurringInvoiceDto toDto(RecurringInvoiceEntity entity);
}
