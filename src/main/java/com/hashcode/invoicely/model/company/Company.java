package com.hashcode.invoicely.model.company;

import com.hashcode.invoicely.model.common.Address;
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
    private Branding branding;
    private Defaults defaults;
}
