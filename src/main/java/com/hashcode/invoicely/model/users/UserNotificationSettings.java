package com.hashcode.invoicely.model.users;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserNotificationSettings {

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

    // Getters & Setters
    // (can use Lombok @Data to simplify)

    // Example:
    public boolean isLoginNotification() {
        return loginNotification;
    }

    public void setLoginNotification(boolean loginNotification) {
        this.loginNotification = loginNotification;
    }

    public String getInvoiceCreated() {
        return invoiceCreated;
    }

    public void setInvoiceCreated(String invoiceCreated) {
        this.invoiceCreated = invoiceCreated;
    }

    // ... add for all other fields
}
