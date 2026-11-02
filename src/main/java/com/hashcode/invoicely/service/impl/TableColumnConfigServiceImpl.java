package com.hashcode.invoicely.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.table.TableColumnConfigDto;
import com.hashcode.invoicely.entity.relational.TableColumnConfigEntity;
import com.hashcode.invoicely.repository.relational.TableColumnConfigRepository;
import com.hashcode.invoicely.service.TableColumnConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableColumnConfigServiceImpl implements TableColumnConfigService {

    private final TableColumnConfigRepository repository;
    private final ObjectMapper objectMapper;

    private static final Map<String, List<String>> SYSTEM_DEFAULT_COLUMNS = Map.of(
            "invoice", List.of("status", "number", "client", "amount", "date", "due_date"),
            "client", List.of("name", "contact_email", "paid_to_date", "balance"),
            "payment", List.of("number", "client", "amount", "date", "type"),
            "quote", List.of("status", "number", "client", "amount", "date", "valid_until"),
            "credit", List.of("status", "number", "client", "amount", "date", "balance"),
            "product", List.of("name", "price", "quantity", "cost"),
            "expense", List.of("vendor", "amount", "date", "category", "status"),
            "project", List.of("name", "client", "due_date", "budgeted_hours"),
            "task", List.of("description", "client", "project", "status", "duration")
    );

    private static final Map<String, List<String>> SYSTEM_LOCKED_COLUMNS = Map.of(
            "invoice", List.of("number", "status"),
            "client", List.of("name"),
            "payment", List.of("number", "amount"),
            "quote", List.of("number", "status"),
            "credit", List.of("number"),
            "product", List.of("name"),
            "expense", List.of("vendor", "amount"),
            "project", List.of("name"),
            "task", List.of("description")
    );

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnConfigDto> getAllTableColumnConfigs(UUID companyId) {
        if (companyId == null) {
            companyId = TenantContext.getCompanyId();
        }

        List<TableColumnConfigEntity> entities = companyId != null
                ? repository.findByCompanyId(companyId)
                : Collections.emptyList();

        Map<String, TableColumnConfigDto> result = new LinkedHashMap<>();

        // 1. Seed defaults
        UUID finalCompanyId = companyId;
        SYSTEM_DEFAULT_COLUMNS.forEach((table, defaults) -> {
            result.put(table, TableColumnConfigDto.builder()
                    .companyId(finalCompanyId)
                    .tableName(table)
                    .defaultColumns(defaults)
                    .lockedColumns(SYSTEM_LOCKED_COLUMNS.getOrDefault(table, Collections.emptyList()))
                    .restrictedForEmployees(Collections.emptyList())
                    .allowEmployeeCustomization(true)
                    .build());
        });

        // 2. Overlay company configs
        for (TableColumnConfigEntity e : entities) {
            result.put(e.getTableName(), toDto(e));
        }

        return new ArrayList<>(result.values());
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnConfigDto getTableColumnConfig(UUID companyId, String tableName) {
        if (companyId == null) {
            companyId = TenantContext.getCompanyId();
        }
        String normalizedTable = tableName != null ? tableName.trim().toLowerCase() : "invoice";

        if (companyId != null) {
            Optional<TableColumnConfigEntity> entityOpt = repository.findByCompanyIdAndTableName(companyId, normalizedTable);
            if (entityOpt.isPresent()) {
                return toDto(entityOpt.get());
            }
        }

        return TableColumnConfigDto.builder()
                .companyId(companyId)
                .tableName(normalizedTable)
                .defaultColumns(SYSTEM_DEFAULT_COLUMNS.getOrDefault(normalizedTable, List.of("number", "status")))
                .lockedColumns(SYSTEM_LOCKED_COLUMNS.getOrDefault(normalizedTable, List.of("number")))
                .restrictedForEmployees(Collections.emptyList())
                .allowEmployeeCustomization(true)
                .build();
    }

    @Override
    @Transactional
    public TableColumnConfigDto saveTableColumnConfig(UUID companyId, String tableName, TableColumnConfigDto dto) {
        final UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        String normalizedTable = tableName != null ? tableName.trim().toLowerCase() : "invoice";

        Optional<TableColumnConfigEntity> existingOpt = effectiveCompanyId != null
                ? repository.findByCompanyIdAndTableName(effectiveCompanyId, normalizedTable)
                : Optional.empty();

        TableColumnConfigEntity entity = existingOpt.orElseGet(() -> TableColumnConfigEntity.builder()
                .companyId(finalCompanyId(effectiveCompanyId))
                .tableName(normalizedTable)
                .build());

        try {
            entity.setDefaultColumns(objectMapper.writeValueAsString(dto.getDefaultColumns() != null ? dto.getDefaultColumns() : Collections.emptyList()));
            entity.setLockedColumns(objectMapper.writeValueAsString(dto.getLockedColumns() != null ? dto.getLockedColumns() : Collections.emptyList()));
            entity.setRestrictedForEmployees(objectMapper.writeValueAsString(dto.getRestrictedForEmployees() != null ? dto.getRestrictedForEmployees() : Collections.emptyList()));
            entity.setAllowEmployeeCustomization(dto.isAllowEmployeeCustomization());
            entity = repository.save(entity);
            return toDto(entity);
        } catch (Exception e) {
            log.error("Failed to save table column config for {}: {}", normalizedTable, e.getMessage());
            throw new RuntimeException("Failed to save table column configuration", e);
        }
    }

    private UUID finalCompanyId(UUID companyId) {
        if (companyId != null) return companyId;
        UUID ctx = TenantContext.getCompanyId();
        if (ctx != null) return ctx;
        throw new IllegalArgumentException("Company ID is required to configure table columns");
    }

    private TableColumnConfigDto toDto(TableColumnConfigEntity e) {
        return TableColumnConfigDto.builder()
                .id(e.getId())
                .companyId(e.getCompanyId())
                .tableName(e.getTableName())
                .defaultColumns(parseList(e.getDefaultColumns()))
                .lockedColumns(parseList(e.getLockedColumns()))
                .restrictedForEmployees(parseList(e.getRestrictedForEmployees()))
                .allowEmployeeCustomization(e.isAllowEmployeeCustomization())
                .build();
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse column list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
