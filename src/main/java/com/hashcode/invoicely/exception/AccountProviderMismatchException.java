package com.hashcode.invoicely.exception;

import lombok.Getter;

@Getter
public class AccountProviderMismatchException extends RuntimeException {

    private final String requiredProvider;

    public AccountProviderMismatchException(String message, String requiredProvider) {
        super(message);
        this.requiredProvider = requiredProvider;
    }
}
