package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.document.DocumentDto;
import com.hashcode.invoicely.entity.relational.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDto toDto(DocumentEntity entity) {
        if (entity == null) {
            return null;
        }
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
}
