package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderDto;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderItemDto;
import com.hashcode.invoicely.entity.relational.PurchaseOrderEntity;
import com.hashcode.invoicely.entity.relational.PurchaseOrderItemEntity;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderService {

    GenericManyResponse<PurchaseOrderDto> getPurchaseOrders(int page, int perPage, String status, UUID vendorId, String filter, String sort);

    PurchaseOrderDto getBlankPurchaseOrder(UUID vendorId);

    PurchaseOrderDto getPurchaseOrderById(UUID id);

    PurchaseOrderDto createPurchaseOrder(PurchaseOrderDto dto);

    PurchaseOrderDto updatePurchaseOrder(UUID id, PurchaseOrderDto dto);

    void deletePurchaseOrder(UUID id);

    PurchaseOrderDto markSent(UUID id);

    List<PurchaseOrderDto> bulkAction(PurchaseOrderBulkActionRequest request);

    PurchaseOrderDto toDto(PurchaseOrderEntity entity);

    PurchaseOrderItemDto toItemDto(PurchaseOrderItemEntity entity);
}
