package com.ledgerguard.infrastructure;

import com.ledgerguard.domain.Rule;
import com.ledgerguard.domain.RuleOperator;
import com.ledgerguard.domain.RuleSeverity;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvRuleReader {
    private static final List<String> REQUIRED_HEADERS =
            List.of("rule_id", "rule_name", "field", "operator", "value", "severity");

    public List<Rule> read(InputStream input) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = format.parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            if (!parser.getHeaderMap().keySet().containsAll(REQUIRED_HEADERS)) {
                throw new IOException("Rules CSV is missing required columns: " + REQUIRED_HEADERS);
            }
            List<Rule> rules = new ArrayList<>();
            Set<String> ruleIds = new HashSet<>();
            for (CSVRecord record : parser) {
                try {
                    Rule rule = new Rule(
                            record.get("rule_id"),
                            record.get("rule_name"),
                            record.get("field"),
                            RuleOperator.fromCsv(record.get("operator")),
                            record.get("value"),
                            RuleSeverity.fromCsv(record.get("severity")));
                    if (!ruleIds.add(rule.ruleId())) {
                        throw new IllegalArgumentException("Duplicate rule_id: " + rule.ruleId());
                    }
                    rules.add(rule);
                } catch (IllegalArgumentException exception) {
                    throw new IOException("Invalid rule at CSV record " + record.getRecordNumber()
                            + ": " + exception.getMessage(), exception);
                }
            }
            return List.copyOf(rules);
        }
    }
}
