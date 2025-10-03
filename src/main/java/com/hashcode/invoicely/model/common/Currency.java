package com.hashcode.invoicely.model.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Currency {
    private String currencyCode;
    private String currencyName;
    private String currencySymbol;
    private String currencyExchangeRate;
    private String currencyExchangeRateName;
    private Boolean isBaseCurrency;
}
