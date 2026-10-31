package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    GenericManyResponse<InvoiceDto> getInvoices(int page, int perPage, String status, UUID clientId, String filter, String sort);

    InvoiceDto getBlankInvoice(UUID clientId);

    InvoiceDto getInvoiceById(UUID id);

    InvoiceDto createInvoice(InvoiceDto dto);

    InvoiceDto updateInvoice(UUID id, InvoiceDto dto);

    List<InvoiceDto> bulkAction(InvoiceBulkActionRequest request);

    InvoiceDto cloneInvoice(UUID id);

    InvoiceDto markSent(UUID id);

    InvoiceDto markPaid(UUID id);

    InvoiceDto toDto(InvoiceEntity entity);
}
