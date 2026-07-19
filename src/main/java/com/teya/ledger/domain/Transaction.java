package com.teya.ledger.domain;

import com.teya.ledger.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final UUID transactionId;
    private final UUID accountId;
    private final String description;
    private final Money money;
    private final TransactionType transactionType;
    private final Instant timestamp;
    private final BigDecimal balanceAfter;

    public Transaction(UUID transactionId,
                       UUID accountId,
                       String description,
                       Money money,
                       TransactionType transactionType,
                       Instant timestamp,
                       BigDecimal balanceAfter) {
        validate(transactionId, accountId, description, money, transactionType, timestamp, balanceAfter);
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.description = description;
        this.money = money;
        this.transactionType = transactionType;
        this.timestamp = timestamp;
        this.balanceAfter = balanceAfter;
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

    public Money getMoney() {
        return money;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    private void validate(
            UUID transactionId,
            UUID accountId,
            String description,
            Money money,
            TransactionType transactionType,
            Instant timestamp,
            BigDecimal balanceAfter
    ) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (description.trim().length() > 35) {
            throw new IllegalArgumentException("Description cannot exceed 35 characters");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (money == null) {
            throw new IllegalArgumentException("Amount must be bigger than zero");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (timestamp.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Timestamp cannot be in the future");
        }
        if (balanceAfter == null) {
            throw new IllegalArgumentException("Balance after transaction cannot be null");
        }
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(description, that.description) &&
                Objects.equals(money, that.money) &&
                transactionType == that.transactionType &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(balanceAfter, that.balanceAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountId, description, money, transactionType, timestamp, balanceAfter);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", accountId=" + accountId +
                ", description='" + description + '\'' +
                ", money=" + money +
                ", transactionType=" + transactionType +
                ", timestamp=" + timestamp +
                ", balanceAfter=" + balanceAfter +
                '}';
    }
}
