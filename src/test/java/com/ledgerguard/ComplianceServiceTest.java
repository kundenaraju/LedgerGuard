package com.ledgerguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ledgerguard.application.ComplianceService;
import com.ledgerguard.domain.Rule;
import com.ledgerguard.domain.RuleBreach;
import com.ledgerguard.domain.RuleOperator;
import com.ledgerguard.domain.RuleSeverity;
import com.ledgerguard.domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComplianceServiceTest {
    private final ComplianceService service = new ComplianceService();

    @Test
    void evaluatesNumericAndMembershipRules() {
        Transaction transaction = transaction("T1", "1500", "US", "2024-05-10T21:45:00Z");
        List<Rule> rules = List.of(
                rule("R1", "amount", RuleOperator.GT, "1000", RuleSeverity.HIGH),
                rule("R2", "country", RuleOperator.IN, "US,CA", RuleSeverity.MEDIUM),
                rule("R3", "timestamp.hour", RuleOperator.GT, "20", RuleSeverity.LOW),
                rule("R4", "amount", RuleOperator.LT, "1000", RuleSeverity.CRITICAL));

        assertEquals(List.of("R1", "R2", "R3"), service.evaluate(List.of(transaction), rules).stream()
                .map(RuleBreach::ruleId)
                .toList());
    }

    @Test
    void returnsNoBreachWhenNoRuleMatches() {
        Transaction transaction = transaction("T1", "100", "GB", "2024-05-10T12:00:00Z");

        assertTrue(service.evaluate(
                List.of(transaction),
                List.of(rule("R1", "amount", RuleOperator.GT, "1000", RuleSeverity.HIGH))).isEmpty());
    }

    @Test
    void rejectsUnsupportedFieldAndMalformedNumericRule() {
        assertThrows(IllegalArgumentException.class,
                () -> rule("R1", "unknown", RuleOperator.EQ, "x", RuleSeverity.LOW));
        assertThrows(IllegalArgumentException.class,
                () -> rule("R2", "amount", RuleOperator.GT, "high", RuleSeverity.LOW));
    }

    @Test
    void supportsEqualityAndInequalityForTextAndNumbers() {
        Transaction transaction = transaction("T1", "100", "US", "2024-05-10T20:00:00Z");
        List<Rule> rules = List.of(
                rule("R1", "amount", RuleOperator.EQ, "100.00", RuleSeverity.LOW),
                rule("R2", "amount", RuleOperator.NEQ, "101", RuleSeverity.LOW),
                rule("R3", "country", RuleOperator.NEQ, "CA", RuleSeverity.LOW),
                rule("R4", "status", RuleOperator.EQ, "pending", RuleSeverity.LOW));

        assertEquals(List.of("R1", "R2", "R3"), service.evaluate(List.of(transaction), rules).stream()
                .map(RuleBreach::ruleId)
                .toList());
    }

    private static Rule rule(String id, String field, RuleOperator operator, String value, RuleSeverity severity) {
        return new Rule(id, id, field, operator, value, severity);
    }

    private static Transaction transaction(String id, String amount, String country, String timestamp) {
        return new Transaction(id, new BigDecimal(amount), country, "retail", "gold", "USD", "settled",
                Instant.parse(timestamp));
    }
}
