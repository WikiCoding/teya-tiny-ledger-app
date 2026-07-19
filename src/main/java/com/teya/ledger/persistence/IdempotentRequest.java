package com.teya.ledger.persistence;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.TransactionResponse;

public record IdempotentRequest(CreateTransactionCommand command, TransactionResponse response) { }
