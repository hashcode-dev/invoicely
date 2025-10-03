package com.hashcode.invoicely.model.company;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Defaults {
    private String invoiceTerms;
    private String invoiceFooter;
    private String quoteTerms;
    private String quoteFooter;
    private String creditTerms;
    private String creditFooter;
    private String purchaseOrderTerms;
    private String purchaseOrderFooter;
}
