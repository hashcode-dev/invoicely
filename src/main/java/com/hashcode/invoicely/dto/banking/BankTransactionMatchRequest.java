package com.hashcode.invoicely.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionMatchRequest {

    @Builder.Default
    private List<BankTransactionMatchItem> transactions = new ArrayList<>();
}
