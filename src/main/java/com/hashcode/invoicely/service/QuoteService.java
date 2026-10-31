package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.QuoteBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.QuoteDto;
import com.hashcode.invoicely.entity.relational.QuoteEntity;

import java.util.List;
import java.util.UUID;

public interface QuoteService {

    GenericManyResponse<QuoteDto> getQuotes(int page, int perPage, String status, UUID clientId, String filter, String sort);

    QuoteDto getBlankQuote(UUID clientId);

    QuoteDto getQuoteById(UUID id);

    QuoteDto createQuote(QuoteDto dto);

    QuoteDto updateQuote(UUID id, QuoteDto dto);

    InvoiceDto convertToInvoice(UUID quoteId);

    List<QuoteDto> bulkAction(QuoteBulkActionRequest request);

    QuoteDto toDto(QuoteEntity entity);
}
