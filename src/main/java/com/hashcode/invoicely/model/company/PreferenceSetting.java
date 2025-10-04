package com.hashcode.invoicely.model.company;

import com.hashcode.invoicely.model.common.Appearance;
import com.hashcode.invoicely.model.common.Currency;
import com.hashcode.invoicely.model.common.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PreferenceSetting {
    private Currency currency;
    private Locale locale;
    private Appearance appearance;
    private boolean mfaEnabled;
    private String accentColor;
    private String firstMonthOfYear;
    private NotificationSettings notificationSettings;
}
