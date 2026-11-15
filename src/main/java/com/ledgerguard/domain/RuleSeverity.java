package com.ledgerguard.domain;

import java.util.Arrays;

public enum RuleSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RuleSeverity fromCsv(String value) {
        return Arrays.stream(values())
                .filter(severity -> severity.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported rule severity: " + value));
    }
}
