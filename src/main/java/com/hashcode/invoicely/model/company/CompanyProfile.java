package com.hashcode.invoicely.model.company;

import com.hashcode.invoicely.model.common.Address;
import com.hashcode.invoicely.model.common.Contact;
import com.hashcode.invoicely.model.common.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CompanyProfile {
    private String profileId;
    private String companyName;
    private String gstNumber;
    private String vatNumber;
    private String companySize;
    private String industry;
    private String classification;
    private String emailSignature;
    private String OAuthEmail;
    private Contact contact;
    private Address address;
    private Branding branding;
    private Defaults defaults;
    private Locale locale;
    private PreferenceSetting preferenceSetting;
}
