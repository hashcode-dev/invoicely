package com.hashcode.invoicely.model.tax;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class TaxRates {
    private String taxId;
    private String taxCode;
    private String taxName;
    private Double taxRate;
}
