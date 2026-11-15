package com.ledgerguard.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record Rule(
        String ruleId,
        String ruleName,
        String field,
        RuleOperator operator,
        String value,
        RuleSeverity severity) {
    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "amount", "timestamp.hour", "country", "merchant_category",
            "customer_tier", "currency", "status");

    public Rule {
        ruleId = requireText(ruleId, "rule_id");
        ruleName = requireText(ruleName, "rule_name");
        field = requireText(field, "field");
        value = requireText(value, "value");
        if (operator == null || severity == null) {
            throw new IllegalArgumentException("Rule operator and severity are required.");
        }
        if (!SUPPORTED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported rule field: " + field);
        }
        if (operator == RuleOperator.IN && allowedValues(value).isEmpty()) {
            throw new IllegalArgumentException("An 'in' rule must have at least one value: " + ruleId);
        }
        boolean numericField = field.equals("amount") || field.equals("timestamp.hour");
        if (numericField && operator == RuleOperator.IN) {
            throw new IllegalArgumentException("The 'in' operator is not supported for field " + field);
        }
        if (!numericField && (operator == RuleOperator.GT || operator == RuleOperator.LT)) {
            throw new IllegalArgumentException("Numeric operator %s is not supported for field %s."
                    .formatted(operator, field));
        }
        if (numericField) {
            try {
                new java.math.BigDecimal(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Rule value must be numeric: " + ruleId, exception);
            }
        }
    }

    public Set<String> allowedValues() {
        return allowedValues(value);
    }

    private static Set<String> allowedValues(String value) {
        return Arrays.stream(value.split(",", -1))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
