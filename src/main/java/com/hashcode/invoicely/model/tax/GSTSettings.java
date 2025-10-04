package com.hashcode.invoicely.model.tax;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class GSTSettings {
    private Boolean gstEnabled;
    private String gstNumber;
    private String gstRegType;
    private String placeOfSupply;
    private String taxRegistrationNumberLabel;
    private String businessLegalName;
    private String businessTradeName;
    private Date registrationDate;
    private Boolean compositionScheme;
    private Boolean reverseCharge;
    private boolean digitalServices;
}
