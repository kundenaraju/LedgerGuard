package com.ledgerguard.domain;

public record RuleBreach(String transactionId, String ruleId, RuleSeverity severity) {
}
