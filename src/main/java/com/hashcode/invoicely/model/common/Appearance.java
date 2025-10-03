package com.hashcode.invoicely.model.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Appearance {
    private Boolean darkModeEnabled;
    private Boolean lightModeEnabled;
    private Long accentColorCode; // Stored as ARGB integer
}
