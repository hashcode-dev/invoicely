package com.hashcode.invoicely.model.users;

import com.hashcode.invoicely.model.common.Currency;
import com.hashcode.invoicely.model.common.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserSettings {
    private Currency currency;
    private Locale locale;
    private String firstMonthOfYear;
}
