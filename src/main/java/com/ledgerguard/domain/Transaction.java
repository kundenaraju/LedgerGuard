package com.ledgerguard.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String transactionId,
        BigDecimal amount,
        String country,
        String merchantCategory,
        String customerTier,
        String currency,
        String status,
        Instant timestamp) {
}
