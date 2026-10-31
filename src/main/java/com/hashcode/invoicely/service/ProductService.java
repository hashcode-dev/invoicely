package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ProductBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ProductDto;
import com.hashcode.invoicely.entity.relational.ProductEntity;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    GenericManyResponse<ProductDto> getProducts(int page, int perPage, String status, String filter, String sort);

    ProductDto getBlankProduct();

    ProductDto getProductById(UUID id);

    ProductDto createProduct(ProductDto dto);

    ProductDto updateProduct(UUID id, ProductDto dto);

    void deleteProduct(UUID id);

    List<ProductDto> bulkAction(ProductBulkActionRequest request);

    ProductDto toDto(ProductEntity entity);
}
