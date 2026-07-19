package com.teya.ledger.persistence.datamodels;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.TransactionResponse;

public record IdempotencyDataModel(CreateTransactionCommand command, TransactionResponse response) { }
