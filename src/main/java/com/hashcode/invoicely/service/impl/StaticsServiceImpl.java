package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.dto.statics.StaticsResponse;
import com.hashcode.invoicely.service.StaticsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StaticsServiceImpl implements StaticsService {

    private final StaticsResponse cachedStatics;

    public StaticsServiceImpl() {
        this.cachedStatics = buildDefaultStatics();
    }

    @Override
    public StaticsResponse getStatics() {
        return cachedStatics;
    }

    private StaticsResponse buildDefaultStatics() {
        List<Map<String, Object>> currencies = new ArrayList<>();
        currencies.add(Map.of("id", "1", "name", "US Dollar", "symbol", "$", "precision", 2, "thousand_separator", ",", "decimal_separator", ".", "code", "USD", "swap_currency_symbol", false, "exchange_rate", 1.0));
        currencies.add(Map.of("id", "2", "name", "British Pound", "symbol", "£", "precision", 2, "thousand_separator", ",", "decimal_separator", ".", "code", "GBP", "swap_currency_symbol", false, "exchange_rate", 0.79));
        currencies.add(Map.of("id", "3", "name", "Euro", "symbol", "€", "precision", 2, "thousand_separator", ".", "decimal_separator", ",", "code", "EUR", "swap_currency_symbol", false, "exchange_rate", 0.92));
        currencies.add(Map.of("id", "4", "name", "Canadian Dollar", "symbol", "CA$", "precision", 2, "thousand_separator", ",", "decimal_separator", ".", "code", "CAD", "swap_currency_symbol", false, "exchange_rate", 1.35));
        currencies.add(Map.of("id", "5", "name", "Australian Dollar", "symbol", "A$", "precision", 2, "thousand_separator", ",", "decimal_separator", ".", "code", "AUD", "swap_currency_symbol", false, "exchange_rate", 1.50));
        currencies.add(Map.of("id", "6", "name", "Indian Rupee", "symbol", "₹", "precision", 2, "thousand_separator", ",", "decimal_separator", ".", "code", "INR", "swap_currency_symbol", false, "exchange_rate", 83.2));

        List<Map<String, Object>> countries = new ArrayList<>();
        countries.add(Map.of("id", "840", "name", "United States", "iso_3166_2", "US", "iso_3166_3", "USA", "currency_code", "USD", "thousand_separator", ",", "decimal_separator", "."));
        countries.add(Map.of("id", "826", "name", "United Kingdom", "iso_3166_2", "GB", "iso_3166_3", "GBR", "currency_code", "GBP", "thousand_separator", ",", "decimal_separator", "."));
        countries.add(Map.of("id", "124", "name", "Canada", "iso_3166_2", "CA", "iso_3166_3", "CAN", "currency_code", "CAD", "thousand_separator", ",", "decimal_separator", "."));
        countries.add(Map.of("id", "356", "name", "India", "iso_3166_2", "IN", "iso_3166_3", "IND", "currency_code", "INR", "thousand_separator", ",", "decimal_separator", "."));
        countries.add(Map.of("id", "36", "name", "Australia", "iso_3166_2", "AU", "iso_3166_3", "AUS", "currency_code", "AUD", "thousand_separator", ",", "decimal_separator", "."));
        countries.add(Map.of("id", "276", "name", "Germany", "iso_3166_2", "DE", "iso_3166_3", "DEU", "currency_code", "EUR", "thousand_separator", ".", "decimal_separator", ","));

        List<Map<String, Object>> dateFormats = new ArrayList<>();
        dateFormats.add(Map.of("id", "1", "format", "YYYY-MM-DD", "picker_format", "Y-m-d", "label", "2026-10-28"));
        dateFormats.add(Map.of("id", "2", "format", "MM/DD/YYYY", "picker_format", "m/d/Y", "label", "10/28/2026"));
        dateFormats.add(Map.of("id", "3", "format", "DD/MM/YYYY", "picker_format", "d/m/Y", "label", "28/10/2026"));
        dateFormats.add(Map.of("id", "4", "format", "DD.MM.YYYY", "picker_format", "d.m.Y", "label", "28.10.2026"));

        List<Map<String, Object>> datetimeFormats = new ArrayList<>();
        datetimeFormats.add(Map.of("id", "1", "format", "YYYY-MM-DD HH:mm:ss", "label", "2026-10-28 14:00:00"));
        datetimeFormats.add(Map.of("id", "2", "format", "MM/DD/YYYY h:mm A", "label", "10/28/2026 2:00 PM"));
        datetimeFormats.add(Map.of("id", "3", "format", "DD/MM/YYYY HH:mm", "label", "28/10/2026 14:00"));

        List<Map<String, Object>> industries = new ArrayList<>();
        industries.add(Map.of("id", "1", "name", "Software & IT Services"));
        industries.add(Map.of("id", "2", "name", "Consulting & Professional Services"));
        industries.add(Map.of("id", "3", "name", "Design & Creative Services"));
        industries.add(Map.of("id", "4", "name", "Financial Services & Accounting"));
        industries.add(Map.of("id", "5", "name", "Retail & E-commerce"));

        List<Map<String, Object>> sizes = new ArrayList<>();
        sizes.add(Map.of("id", "1", "name", "1-10 Employees"));
        sizes.add(Map.of("id", "2", "name", "11-50 Employees"));
        sizes.add(Map.of("id", "3", "name", "51-200 Employees"));
        sizes.add(Map.of("id", "4", "name", "201+ Employees"));

        List<Map<String, Object>> languages = new ArrayList<>();
        languages.add(Map.of("id", "1", "name", "English", "locale", "en"));
        languages.add(Map.of("id", "2", "name", "Spanish", "locale", "es"));
        languages.add(Map.of("id", "3", "name", "French", "locale", "fr"));
        languages.add(Map.of("id", "4", "name", "German", "locale", "de"));

        List<Map<String, Object>> paymentTypes = new ArrayList<>();
        paymentTypes.add(Map.of("id", "1", "name", "Credit Card", "gateway_type_id", 1));
        paymentTypes.add(Map.of("id", "2", "name", "Bank Transfer", "gateway_type_id", 2));
        paymentTypes.add(Map.of("id", "3", "name", "PayPal", "gateway_type_id", 3));
        paymentTypes.add(Map.of("id", "4", "name", "Cash", "gateway_type_id", 4));
        paymentTypes.add(Map.of("id", "5", "name", "Check", "gateway_type_id", 5));

        List<Map<String, Object>> timezones = new ArrayList<>();
        timezones.add(Map.of("id", "1", "name", "America/New_York (EST/EDT)", "location", "America/New_York", "utc_offset", -18000));
        timezones.add(Map.of("id", "2", "name", "America/Chicago (CST/CDT)", "location", "America/Chicago", "utc_offset", -21600));
        timezones.add(Map.of("id", "3", "name", "America/Los_Angeles (PST/PDT)", "location", "America/Los_Angeles", "utc_offset", -28800));
        timezones.add(Map.of("id", "4", "name", "Europe/London (GMT/BST)", "location", "Europe/London", "utc_offset", 0));
        timezones.add(Map.of("id", "5", "name", "Europe/Berlin (CET/CEST)", "location", "Europe/Berlin", "utc_offset", 3600));
        timezones.add(Map.of("id", "6", "name", "Asia/Kolkata (IST)", "location", "Asia/Kolkata", "utc_offset", 19800));
        timezones.add(Map.of("id", "7", "name", "Asia/Singapore (SGT)", "location", "Asia/Singapore", "utc_offset", 28800));

        Map<String, Object> templates = new HashMap<>();
        templates.put("invoice", Map.of("subject", "Invoice :number from :company", "body", "Hi :client,\n\nPlease find your invoice :number attached.\n\nThank you for your business!"));
        templates.put("quote", Map.of("subject", "Quote :number from :company", "body", "Hi :client,\n\nPlease find your estimate :number attached.\n\nBest regards!"));
        templates.put("payment", Map.of("subject", "Payment Receipt :number", "body", "Thank you for your payment of :amount."));

        return StaticsResponse.builder()
                .currencies(currencies)
                .countries(countries)
                .dateFormats(dateFormats)
                .datetimeFormats(datetimeFormats)
                .industries(industries)
                .sizes(sizes)
                .languages(languages)
                .paymentTypes(paymentTypes)
                .timezones(timezones)
                .templates(templates)
                .licenseKey("INVOICELY-ENTERPRISE-PRO")
                .build();
    }
}
