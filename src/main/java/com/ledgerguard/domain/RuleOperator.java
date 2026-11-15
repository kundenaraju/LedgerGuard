package com.ledgerguard.domain;

import java.util.Arrays;

public enum RuleOperator {
    EQ("eq"),
    NEQ("neq"),
    GT("gt"),
    LT("lt"),
    IN("in");

    private final String csvValue;

    RuleOperator(String csvValue) {
        this.csvValue = csvValue;
    }

    public static RuleOperator fromCsv(String value) {
        return Arrays.stream(values())
                .filter(operator -> operator.csvValue.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported rule operator: " + value));
    }
}
