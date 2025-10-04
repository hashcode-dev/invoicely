package com.hashcode.invoicely.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserRole {
    private String userRoleId;
    private String roleName;
    private String roleDescription;
}
