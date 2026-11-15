package com.ledgerguard.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerguard.domain.Transaction;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JsonTransactionReader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Transaction> read(InputStream input) throws IOException {
        JsonNode root;
        try {
            root = objectMapper.readTree(input);
        } catch (JsonProcessingException exception) {
            throw new IOException("Transactions file contains invalid JSON: " + exception.getOriginalMessage(),
                    exception);
        }
        if (root == null || !root.isObject() || !root.path("transactions").isArray()) {
            throw new IOException("Transactions JSON must contain a 'transactions' array.");
        }

        List<Transaction> transactions = new ArrayList<>();
        Set<String> transactionIds = new HashSet<>();
        int row = 0;
        for (JsonNode node : root.path("transactions")) {
            row++;
            if (!node.isObject()) {
                throw new IOException("Transaction at array index " + (row - 1) + " must be an object.");
            }
            Transaction transaction = parseTransaction(node, row);
            if (!transactionIds.add(transaction.transactionId())) {
                throw new IOException("Duplicate transaction_id at array index " + (row - 1) + ": "
                        + transaction.transactionId());
            }
            transactions.add(transaction);
        }
        return List.copyOf(transactions);
    }

    private Transaction parseTransaction(JsonNode node, int row) throws IOException {
        String id = requiredText(node, "transaction_id", row);
        JsonNode amountNode = node.get("amount");
        if (amountNode == null || !amountNode.isNumber()) {
            throw new IOException("Transaction row " + row + " must have a numeric amount.");
        }
        Instant timestamp = null;
        String timestampText = optionalText(node, "timestamp");
        if (timestampText != null) {
            try {
                timestamp = Instant.parse(timestampText);
            } catch (DateTimeParseException exception) {
                throw new IOException("Transaction row " + row + " has an invalid UTC timestamp.", exception);
            }
        }
        return new Transaction(
                id,
                amountNode.decimalValue(),
                optionalText(node, "country"),
                optionalText(node, "merchant_category"),
                optionalText(node, "customer_tier"),
                optionalText(node, "currency"),
                optionalText(node, "status"),
                timestamp);
    }

    private String requiredText(JsonNode node, String name, int row) throws IOException {
        String value = optionalText(node, name);
        if (value == null || value.isBlank()) {
            throw new IOException("Transaction row " + row + " must have a non-blank " + name + ".");
        }
        return value;
    }

    private String optionalText(JsonNode node, String name) throws IOException {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IOException("Transaction field '" + name + "' must be a string.");
        }
        return value.textValue();
    }
}
