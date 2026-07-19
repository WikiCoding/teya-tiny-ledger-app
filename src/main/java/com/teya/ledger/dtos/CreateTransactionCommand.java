package com.teya.ledger.dtos;

import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record CreateTransactionCommand(UUID transactionId,
                                       UUID accountId,
                                       String description,
                                       TransactionType transactionType,
                                       Money money,
                                       Instant timestamp) {
}
