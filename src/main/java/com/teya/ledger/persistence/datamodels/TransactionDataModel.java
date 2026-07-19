package com.teya.ledger.persistence.datamodels;

import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDataModel {
    private final UUID transactionId;
    private final UUID accountId;
    private final String description;
    private final BigDecimal amount;
    private final String transactionType;
    private final Instant timestamp;
    private final BigDecimal balanceAfter;

    public TransactionDataModel(Transaction transaction) {
        this.transactionId = transaction.getTransactionId();
        this.accountId = transaction.getAccountId();
        this.description = transaction.getDescription();
        this.amount = transaction.getMoney().getAmount();
        this.transactionType = transaction.getTransactionType().toString();
        this.timestamp = transaction.getTimestamp();
        this.balanceAfter = transaction.getBalanceAfter();
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public static Transaction toDomain(TransactionDataModel dataModel) {
        return new Transaction(
                dataModel.getTransactionId(),
                dataModel.getAccountId(),
                dataModel.getDescription(),
                new Money(dataModel.getAmount()),
                TransactionType.valueOf(dataModel.getTransactionType()),
                dataModel.getTimestamp(),
                dataModel.getBalanceAfter()
        );
    }
}
