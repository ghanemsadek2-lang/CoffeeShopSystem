package com.coffeeshop.service;

/** Outcomes safe for an authentication caller to distinguish. */
public enum LoginStatus {
    SUCCESS,
    INVALID_INPUT,
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED
}
