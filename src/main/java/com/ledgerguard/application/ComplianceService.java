package com.ledgerguard.application;

import com.ledgerguard.domain.Rule;
import com.ledgerguard.domain.RuleBreach;
import com.ledgerguard.domain.RuleOperator;
import com.ledgerguard.domain.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ComplianceService {
    public List<RuleBreach> evaluate(List<Transaction> transactions, List<Rule> rules) {
        Objects.requireNonNull(transactions, "transactions must not be null");
        Objects.requireNonNull(rules, "rules must not be null");

        List<RuleBreach> breaches = new ArrayList<>();
        for (Transaction transaction : transactions) {
            for (Rule rule : rules) {
                if (matches(transaction, rule)) {
                    breaches.add(new RuleBreach(transaction.transactionId(), rule.ruleId(), rule.severity()));
                }
            }
        }
        return List.copyOf(breaches);
    }

    private boolean matches(Transaction transaction, Rule rule) {
        return switch (rule.field()) {
            case "amount" -> compareNumber(transaction.amount(), rule);
            case "timestamp.hour" -> transaction.timestamp() != null
                    && compareNumber(BigDecimal.valueOf(transaction.timestamp().atOffset(java.time.ZoneOffset.UTC)
                            .getHour()), rule);
            case "country" -> compareText(transaction.country(), rule);
            case "merchant_category" -> compareText(transaction.merchantCategory(), rule);
            case "customer_tier" -> compareText(transaction.customerTier(), rule);
            case "currency" -> compareText(transaction.currency(), rule);
            case "status" -> compareText(transaction.status(), rule);
            default -> false;
        };
    }

    private boolean compareText(String actual, Rule rule) {
        if (actual == null) {
            return false;
        }
        return switch (rule.operator()) {
            case EQ -> actual.equals(rule.value());
            case NEQ -> !actual.equals(rule.value());
            case IN -> rule.allowedValues().contains(actual);
            case GT, LT -> false;
        };
    }

    private boolean compareNumber(BigDecimal actual, Rule rule) {
        if (actual == null) {
            return false;
        }
        BigDecimal expected = new BigDecimal(rule.value());
        int comparison = actual.compareTo(expected);
        return switch (rule.operator()) {
            case GT -> comparison > 0;
            case LT -> comparison < 0;
            case EQ -> comparison == 0;
            case NEQ -> comparison != 0;
            case IN -> false;
        };
    }
}
