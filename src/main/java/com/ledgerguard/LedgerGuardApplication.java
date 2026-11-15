package com.ledgerguard;

import com.ledgerguard.application.ComplianceService;
import com.ledgerguard.domain.Rule;
import com.ledgerguard.domain.RuleBreach;
import com.ledgerguard.domain.Transaction;
import com.ledgerguard.infrastructure.CsvRuleReader;
import com.ledgerguard.infrastructure.JsonTransactionReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class LedgerGuardApplication {
    private LedgerGuardApplication() {
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = Arguments.parse(args);
            List<Transaction> transactions = loadTransactions(arguments.transactionsPath());
            var rules = loadRules(arguments.rulesPath());
            List<RuleBreach> breaches = new ComplianceService().evaluate(transactions, rules);
            printResults(transactions, breaches);
        } catch (IllegalArgumentException | IOException exception) {
            System.err.println("LedgerGuard: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static List<Transaction> loadTransactions(Path path) throws IOException {
        if (path != null) {
            try (InputStream input = Files.newInputStream(path)) {
                return new JsonTransactionReader().read(input);
            }
        }
        try (InputStream input = requiredResource("/transactions.json")) {
            return new JsonTransactionReader().read(input);
        }
    }

    private static List<Rule> loadRules(Path path) throws IOException {
        if (path != null) {
            try (InputStream input = Files.newInputStream(path)) {
                return new CsvRuleReader().read(input);
            }
        }
        try (InputStream input = requiredResource("/rules.csv")) {
            return new CsvRuleReader().read(input);
        }
    }

    private static InputStream requiredResource(String name) throws IOException {
        InputStream input = LedgerGuardApplication.class.getResourceAsStream(name);
        if (input == null) {
            throw new IOException("Bundled resource was not found: " + name);
        }
        return input;
    }

    private static void printResults(List<Transaction> transactions, List<RuleBreach> breaches) {
        Map<String, List<RuleBreach>> breachesByTransaction = breaches.stream()
                .collect(Collectors.groupingBy(RuleBreach::transactionId));
        System.out.printf("%-16s %s%n", "Transaction", "Breached Rules");
        for (Transaction transaction : transactions) {
            String result = breachesByTransaction.getOrDefault(transaction.transactionId(), List.of()).stream()
                    .map(breach -> "%s (%s)".formatted(breach.ruleId(), breach.severity()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("none");
            System.out.printf("%-16s %s%n", transaction.transactionId(), result);
        }
    }

    private record Arguments(Path transactionsPath, Path rulesPath) {
        private static Arguments parse(String[] args) {
            Path transactions = null;
            Path rules = null;
            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--transactions" -> transactions = valueAfter(args, ++index, "--transactions");
                    case "--rules" -> rules = valueAfter(args, ++index, "--rules");
                    case "--help", "-h" -> {
                        printUsage();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[index]
                            + ". Use --help for usage.");
                }
            }
            return new Arguments(transactions, rules);
        }

        private static Path valueAfter(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException("Missing path after " + option);
            }
            return Path.of(args[index]);
        }

        private static void printUsage() {
            System.out.println("""
                    Usage: java -jar ledgerguard.jar [options]
                      --transactions <path>  JSON transaction input (default: bundled sample)
                      --rules <path>         CSV rule input (default: bundled sample)
                      --help                 Show this help
                    """);
        }
    }
}
