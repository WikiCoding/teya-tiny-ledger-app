package com.teya.ledger.persistence;

import com.teya.ledger.domain.Transaction;
import com.teya.ledger.exceptions.DuplicateTransactionException;
import com.teya.ledger.persistence.datamodels.TransactionDataModel;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryTransactionsRepository implements TransactionsRepository {
    private final ConcurrentMap<UUID, TransactionDataModel> transactions;

    public InMemoryTransactionsRepository() {
        this.transactions = new ConcurrentHashMap<>();
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionDataModel existing = transactions.putIfAbsent(
                transaction.getTransactionId(),
                new TransactionDataModel(transaction)
        );

        if (existing != null) {
            throw new DuplicateTransactionException("Transaction with ID " + transaction.getTransactionId() + " already exists");
        }
        return transaction;
    }

    @Override
    public List<Transaction> findAllSortedByTimestampDesc() {
        return List.copyOf(transactions.values()).stream()
                .sorted(Comparator.comparing(TransactionDataModel::getTimestamp).reversed()
                        .thenComparing(TransactionDataModel::getTransactionId))
                .map(TransactionDataModel::toDomain).toList();
    }
}
