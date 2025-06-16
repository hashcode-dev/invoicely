package com.hashcode.invoicely.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Company {
    private String companyName;
    private String companyNumber;
    private String vatNumber;
    private String website;
    private String email;
    private String companyPhone;
    private String companySize;
    private String industry;
    private String classification;
    private Address address;
    private String logoUrl;
    private Defaults defaults;
}
