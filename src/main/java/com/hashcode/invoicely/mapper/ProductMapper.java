package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.procurement.ProductDto;
import com.hashcode.invoicely.entity.relational.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ProductMapper {

    public ProductDto toDto(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProductDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .productKey(entity.getProductKey())
                .notes(entity.getNotes())
                .cost(entity.getCost())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
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
                .inStockQuantity(entity.getInStockQuantity())
                .stockNotification(entity.isStockNotification())
                .stockNotificationThreshold(entity.getStockNotificationThreshold())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .documents(Collections.emptyList())
                .build();
    }
}
