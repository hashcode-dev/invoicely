package com.hashcode.invoicely.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Contact {
    private String email;
    private String website;
    private String faxNumber;
    private List<PhoneDetails> phoneDetails;
}
