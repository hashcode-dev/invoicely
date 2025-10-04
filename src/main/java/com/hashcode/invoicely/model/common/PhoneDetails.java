package com.hashcode.invoicely.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PhoneDetails {
    private String phoneNumber;
    private String phoneCountryCode;
    private String phoneCountryIsoCode;
    private String phoneType;
}
