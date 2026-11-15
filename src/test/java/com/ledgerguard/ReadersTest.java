package com.ledgerguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ledgerguard.application.ComplianceService;
import com.ledgerguard.domain.RuleBreach;
import com.ledgerguard.infrastructure.CsvRuleReader;
import com.ledgerguard.infrastructure.JsonTransactionReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadersTest {
    private final JsonTransactionReader transactionReader = new JsonTransactionReader();
    private final CsvRuleReader ruleReader = new CsvRuleReader();

    @Test
    void readsTransactionJsonAndRejectsInvalidRows() throws Exception {
        String json = """
                {"transactions":[
                  {"transaction_id":"T1","amount":42.50,"country":"US","timestamp":"2024-05-10T21:00:00Z"}
                ]}
                """;
        var transactions = transactionReader.read(input(json));

        assertEquals(1, transactions.size());
        assertEquals("T1", transactions.get(0).transactionId());
        assertEquals("42.5", transactions.get(0).amount().toPlainString());

        assertThrows(IOException.class, () -> transactionReader.read(input("{\"transactions\":[null]}")));
        assertThrows(IOException.class, () -> transactionReader.read(input("{bad json")));
        assertThrows(IOException.class, () -> transactionReader.read(input("""
                {"transactions":[
                  {"transaction_id":"T1","amount":1},
                  {"transaction_id":"T1","amount":2}
                ]}
                """)));
    }

    @Test
    void readsQuotedCommaAndValidatesRuleRows() throws Exception {
        String csv = """
                rule_id,rule_name,field,operator,value,severity
                R1,North America,country,in,"US,CA",MEDIUM
                """;
        var rules = ruleReader.read(input(csv));

        assertEquals(1, rules.size());
        assertTrue(rules.get(0).allowedValues().contains("CA"));
        assertThrows(IOException.class, () -> ruleReader.read(input(
                "rule_id,field,operator,value,severity\nR1,country,wat,US,INVALID\n")));
        assertThrows(IOException.class, () -> ruleReader.read(input("""
                rule_id,rule_name,field,operator,value,severity
                R1,first,country,eq,US,LOW
                R1,duplicate,country,eq,CA,LOW
                """)));
    }

    @Test
    void loadsBundledSampleFiles() throws Exception {
        try (var transactions = getClass().getResourceAsStream("/transactions.json");
             var rules = getClass().getResourceAsStream("/rules.csv")) {
            var sampleTransactions = transactionReader.read(transactions);
            var sampleRules = ruleReader.read(rules);
            assertEquals(8, sampleTransactions.size());
            assertEquals(8, sampleRules.size());
            var breaches = new ComplianceService().evaluate(sampleTransactions, sampleRules);
            assertEquals(27, breaches.size());
            assertEquals(
                    List.of("R001", "R003", "R006"),
                    breaches.stream()
                            .filter(breach -> breach.transactionId().equals("TXN001"))
                            .map(RuleBreach::ruleId)
                            .toList());
        }
    }

    private static ByteArrayInputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
