package com.hashcode.invoicely.model.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Locale {
    private String language;
    private String languageCode;
    private String country;
    private String timeZone;
    private String dateFormat;
    private boolean use24HourTime;
}
