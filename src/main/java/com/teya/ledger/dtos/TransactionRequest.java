package com.teya.ledger.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRequest(UUID accountId,
                                 String description,
                                 String transactionType,
                                 BigDecimal amount,
                                 Instant timestamp) { }
