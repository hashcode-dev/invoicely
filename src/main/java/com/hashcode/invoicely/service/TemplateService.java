package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.document.TemplateRequest;
import com.hashcode.invoicely.dto.document.TemplateResponse;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final PdfRenderingService pdfRenderingService;

    @Transactional(readOnly = true)
    public TemplateResponse renderTemplate(TemplateRequest request) {
        UUID companyId = TenantContext.getCompanyId();

        String rawSubject = request.getSubject() != null ? request.getSubject() : "Invoice from $company.name";
        String rawBody = request.getBody() != null ? request.getBody() : "Dear $client.name, please find your invoice $invoice.number.";

        String companyName = "Invoicely";
        if (companyId != null) {
            companyName = companyRepository.findById(companyId)
                    .map(CompanyEntity::getCompanyName)
                    .orElse("Invoicely");
        }

        String clientName = "Valued Customer";
        String invoiceNumber = "INV-0001";
        String invoiceAmount = "$0.00";
        String dueDate = "Net 30";

        if (StringUtils.hasText(request.getEntityId())) {
            try {
                UUID entityUuid = UUID.fromString(request.getEntityId());
                if ("invoice".equalsIgnoreCase(request.getEntity())) {
                    InvoiceEntity invoice = invoiceRepository.findByCompanyIdAndId(companyId, entityUuid).orElse(null);
                    if (invoice != null) {
                        invoiceNumber = invoice.getNumber();
                        invoiceAmount = "$" + invoice.getAmount().toPlainString();
                        dueDate = invoice.getDueDate() != null ? invoice.getDueDate().toString() : "Due on receipt";

                        if (invoice.getClientId() != null) {
                            clientName = clientRepository.findByCompanyIdAndId(companyId, invoice.getClientId())
                                    .map(ClientEntity::getName)
                                    .orElse("Valued Customer");
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String subject = rawSubject
                .replace("$company.name", companyName)
                .replace("$company", companyName)
                .replace("$client.name", clientName)
                .replace("$client", clientName)
                .replace("$invoice.number", invoiceNumber)
                .replace("$invoice", invoiceNumber)
                .replace("$amount", invoiceAmount)
                .replace("$due_date", dueDate);

        String body = rawBody
                .replace("$company.name", companyName)
                .replace("$company", companyName)
                .replace("$client.name", clientName)
                .replace("$client", clientName)
                .replace("$invoice.number", invoiceNumber)
                .replace("$invoice", invoiceNumber)
                .replace("$amount", invoiceAmount)
                .replace("$due_date", dueDate);

        return TemplateResponse.builder()
                .subject(subject)
                .body(body)
                .rawBody(rawBody)
                .build();
    }

    public byte[] renderPreview(String hash) {
        return pdfRenderingService.generatePdf("Template Preview", "Preview for template token: " + hash, Map.of("hash", hash));
    }
}
