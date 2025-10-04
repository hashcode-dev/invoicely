package com.hashcode.invoicely.model.users;

import com.hashcode.invoicely.model.common.Contact;
import com.hashcode.invoicely.model.common.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserDetails {
    private String userId;
    private String firstName;
    private String lastName;
    private UserRole userRole;
    private Contact contact;
}
