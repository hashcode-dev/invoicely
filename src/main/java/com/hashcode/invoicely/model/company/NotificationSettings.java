package com.hashcode.invoicely.model.company;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class NotificationSettings {
    // Toggles
    private boolean loginNotification;
    private boolean taskAssignedNotification;
    private boolean disableRecurringPaymentNotification;
    // Global setting
    private String allEvents; // e.g., "Custom", "All"
    // Invoice related
    private String invoiceCreated;   // None / All records
    private String invoiceSent;
    private String invoiceViewed;
    private String invoiceLate;
    // Inventory
    private String inventoryThreshold;
    // Payment
    private String paymentSuccess;
    private String paymentFailure;
    private String paymentManual;
    // Purchase Order
    private String purchaseOrderCreated;
    private String purchaseOrderSent;
    private String purchaseOrderViewed;
    private String purchaseOrderAccepted;
    // Quote
    private String quoteCreated;
    private String quoteSent;
    private String quoteViewed;
    private String quoteApproved;
    private String quoteExpired;
    // Credit
    private String creditCreated;
    private String creditSent;
    private String creditViewed;
}
