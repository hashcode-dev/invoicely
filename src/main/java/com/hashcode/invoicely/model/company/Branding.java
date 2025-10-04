package com.hashcode.invoicely.model.company;

import com.hashcode.invoicely.model.common.Appearance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Branding {
    private String companyLogoURL;
    private Appearance appearance;
}
