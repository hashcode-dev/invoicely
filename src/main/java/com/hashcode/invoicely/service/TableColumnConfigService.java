package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.table.TableColumnConfigDto;

import java.util.List;
import java.util.UUID;

public interface TableColumnConfigService {
    List<TableColumnConfigDto> getAllTableColumnConfigs(UUID companyId);
    TableColumnConfigDto getTableColumnConfig(UUID companyId, String tableName);
    TableColumnConfigDto saveTableColumnConfig(UUID companyId, String tableName, TableColumnConfigDto dto);
}
