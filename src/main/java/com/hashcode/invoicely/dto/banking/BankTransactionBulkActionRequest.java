package com.hashcode.invoicely.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionBulkActionRequest {

    private String action; // archive, restore, delete, convert_to_payment, convert_to_expense

    private List<UUID> ids;
}
