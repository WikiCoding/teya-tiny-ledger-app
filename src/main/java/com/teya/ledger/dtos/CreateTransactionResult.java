package com.teya.ledger.dtos;

public record CreateTransactionResult(TransactionResponse response, boolean replayed) { }
