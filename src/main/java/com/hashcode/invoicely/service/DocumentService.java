package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.document.DocumentBulkActionRequest;
import com.hashcode.invoicely.dto.document.DocumentDto;
import com.hashcode.invoicely.dto.document.DocumentVisibilityRequest;
import com.hashcode.invoicely.entity.relational.DocumentEntity;
import com.hashcode.invoicely.repository.relational.DocumentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public GenericManyResponse<DocumentDto> getDocuments(int page, int perPage, String status, Boolean companyDocuments, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<DocumentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DocumentEntity> pageResult = documentRepository.findAll(spec, pageable);
        List<DocumentDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public DocumentDto getDocumentById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        DocumentEntity entity = documentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public DocumentDto getDocumentByHash(String hash) {
        DocumentEntity entity = documentRepository.findByHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with hash: " + hash));
        return toDto(entity);
    }

    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, UUID clientId, UUID vendorId, UUID invoiceId, UUID expenseId) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String originalName = file != null && StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "document_" + System.currentTimeMillis() + ".pdf";

        long size = file != null ? file.getSize() : 0L;
        String contentType = file != null && StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/pdf";

        String hash = generateHash(originalName + companyId + System.nanoTime());

        DocumentEntity entity = DocumentEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .clientId(clientId)
                .vendorId(vendorId)
                .invoiceId(invoiceId)
                .expenseId(expenseId)
                .name(originalName)
                .type(contentType)
                .size(size)
                .width(0)
                .height(0)
                .hash(hash)
                .disk("local")
                .url("/api/v1/documents/" + hash + "/download")
                .isDefault(false)
                .isPublic(false)
                .isDeleted(false)
                .build();

        DocumentEntity saved = documentRepository.save(entity);
        log.info("Uploaded document {} ({}) with hash {} for company {}", saved.getName(), saved.getId(), saved.getHash(), companyId);
        return toDto(saved);
    }

    @Transactional
    public DocumentDto updateVisibility(UUID id, DocumentVisibilityRequest request) {
        UUID companyId = getRequiredCompanyId();
        DocumentEntity entity = documentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        if (request != null && request.getIsPublic() != null) {
            entity.setIsPublic(request.getIsPublic());
        }

        DocumentEntity saved = documentRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteDocument(UUID id) {
        UUID companyId = getRequiredCompanyId();
        DocumentEntity entity = documentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        entity.setIsDeleted(true);
        documentRepository.save(entity);
        log.info("Soft deleted document {} for company {}", id, companyId);
    }

    @Transactional
    public int handleBulkAction(DocumentBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }

        List<DocumentEntity> entities = documentRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        for (DocumentEntity entity : entities) {
            switch (action) {
                case "delete" -> entity.setIsDeleted(true);
                case "archive" -> {
                    entity.setArchivedAt(Instant.now());
                    entity.setIsDeleted(false);
                }
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setIsDeleted(false);
                }
                case "download" -> log.info("Bulk download document {}", entity.getId());
                default -> log.warn("Unknown document bulk action: {}", action);
            }
        }

        documentRepository.saveAll(entities);
        return entities.size();
    }

    public DocumentDto toDto(DocumentEntity entity) {
        if (entity == null) return null;
        return DocumentDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .size(entity.getSize())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .hash(entity.getHash())
                .disk(entity.getDisk())
                .url(entity.getUrl())
                .preview(entity.getPreview())
                .isDefault(Boolean.TRUE.equals(entity.getIsDefault()))
                .isPublic(Boolean.TRUE.equals(entity.getIsPublic()))
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .clientId(entity.getClientId())
                .vendorId(entity.getVendorId())
                .projectId(entity.getProjectId())
                .invoiceId(entity.getInvoiceId())
                .expenseId(entity.getExpenseId())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .build();
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
