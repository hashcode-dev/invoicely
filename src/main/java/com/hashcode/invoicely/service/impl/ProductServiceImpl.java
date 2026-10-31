package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ProductBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ProductDto;
import com.hashcode.invoicely.entity.relational.ProductEntity;
import com.hashcode.invoicely.mapper.ProductMapper;
import com.hashcode.invoicely.repository.relational.ProductRepository;
import com.hashcode.invoicely.service.ProductService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<ProductDto> getProducts(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 500;

        Sort sorting = Sort.by(Sort.Direction.ASC, "productKey");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("productKey")), pattern),
                        cb.like(cb.lower(root.get("notes")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ProductEntity> paged = productRepository.findAll(spec, pageable);
        List<ProductDto> dtos = paged.getContent().stream().map(productMapper::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, paged.getTotalElements(), paged.getTotalPages(), pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getBlankProduct() {
        return ProductDto.builder()
                .productKey("")
                .notes("")
                .cost(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .price(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .quantity(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .taxRate1(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate2(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate3(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .inStockQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .stockNotification(false)
                .stockNotificationThreshold(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .isDeleted(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ProductEntity entity = productRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return productMapper.toDto(entity);
    }

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        ProductEntity entity = ProductEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .productKey(dto.getProductKey() != null ? dto.getProductKey().trim() : "")
                .notes(dto.getNotes())
                .cost(dto.getCost() != null ? dto.getCost().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .price(dto.getPrice() != null ? dto.getPrice().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .quantity(dto.getQuantity() != null ? dto.getQuantity().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .taxName1(dto.getTaxName1())
                .taxRate1(dto.getTaxRate1() != null ? dto.getTaxRate1().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxName2(dto.getTaxName2())
                .taxRate2(dto.getTaxRate2() != null ? dto.getTaxRate2().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxName3(dto.getTaxName3())
                .taxRate3(dto.getTaxRate3() != null ? dto.getTaxRate3().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .inStockQuantity(dto.getInStockQuantity() != null ? dto.getInStockQuantity().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .stockNotification(dto.isStockNotification())
                .stockNotificationThreshold(dto.getStockNotificationThreshold() != null ? dto.getStockNotificationThreshold().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .isDeleted(false)
                .build();

        ProductEntity saved = productRepository.save(entity);
        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(UUID id, ProductDto dto) {
        UUID companyId = getRequiredCompanyId();
        ProductEntity entity = productRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (dto.getProductKey() != null) {
            entity.setProductKey(dto.getProductKey().trim());
        }
        if (dto.getNotes() != null) {
            entity.setNotes(dto.getNotes());
        }
        if (dto.getCost() != null) {
            entity.setCost(dto.getCost().setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice().setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getQuantity() != null) {
            entity.setQuantity(dto.getQuantity().setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getTaxName1() != null) entity.setTaxName1(dto.getTaxName1());
        if (dto.getTaxRate1() != null) entity.setTaxRate1(dto.getTaxRate1().setScale(4, RoundingMode.HALF_UP));
        if (dto.getTaxName2() != null) entity.setTaxName2(dto.getTaxName2());
        if (dto.getTaxRate2() != null) entity.setTaxRate2(dto.getTaxRate2().setScale(4, RoundingMode.HALF_UP));
        if (dto.getTaxName3() != null) entity.setTaxName3(dto.getTaxName3());
        if (dto.getTaxRate3() != null) entity.setTaxRate3(dto.getTaxRate3().setScale(4, RoundingMode.HALF_UP));
        if (dto.getCustomValue1() != null) entity.setCustomValue1(dto.getCustomValue1());
        if (dto.getCustomValue2() != null) entity.setCustomValue2(dto.getCustomValue2());
        if (dto.getCustomValue3() != null) entity.setCustomValue3(dto.getCustomValue3());
        if (dto.getCustomValue4() != null) entity.setCustomValue4(dto.getCustomValue4());
        if (dto.getInStockQuantity() != null) entity.setInStockQuantity(dto.getInStockQuantity().setScale(4, RoundingMode.HALF_UP));
        entity.setStockNotification(dto.isStockNotification());
        if (dto.getStockNotificationThreshold() != null) entity.setStockNotificationThreshold(dto.getStockNotificationThreshold().setScale(4, RoundingMode.HALF_UP));

        ProductEntity updated = productRepository.save(entity);
        return productMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ProductEntity entity = productRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        entity.setDeleted(true);
        productRepository.save(entity);
    }

    @Override
    @Transactional
    public List<ProductDto> bulkAction(ProductBulkActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();
        }
        UUID companyId = getRequiredCompanyId();
        List<ProductEntity> entities = productRepository.findByCompanyIdAndIdIn(companyId, request.getIds());

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        for (ProductEntity entity : entities) {
            switch (action) {
                case "archive" -> entity.setArchivedAt(Instant.now());
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setDeleted(false);
                }
                case "delete" -> entity.setDeleted(true);
                default -> throw new IllegalArgumentException("Unsupported bulk action: " + action);
            }
        }
        productRepository.saveAll(entities);
        return entities.stream().map(productMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public ProductDto toDto(ProductEntity entity) {
        return productMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
