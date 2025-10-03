package com.hashcode.invoicely.model.users;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserDetails {
    private String firstName;
    private String lastName;
    private String email;
    private String language;
    private String phone;
    private String emailSignature;
    private String OAuthEmail;
    private boolean mfaEnabled;
    private String accentColor;
}
