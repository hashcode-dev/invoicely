package com.hashcode.invoicely.model.users;

import com.hashcode.invoicely.model.common.Currency;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserSettings {
    private Currency currency;
    private String language;             // e.g. "English"
    private String timeZone;             // e.g. "Pacific/Midway"
    private String dateFormat;           // e.g. "01/Oct/2025"

    private boolean use24HourTime;       // true if 24 Hour Time is enabled
    private boolean rappenRounding;      // true if Rappen Rounding enabled
    private boolean decimalComma;        // true if Decimal Comma enabled

    private String firstMonthOfYear;
}
