package com.hashcode.invoicely.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Address {
    private String addressLine1;
    private String addressLine2;
    private String townOrCity;
    private String county;
    private String postcode;
    private String country;
}
