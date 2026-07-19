package com.teya.ledger.persistence;

import com.teya.ledger.domain.Transaction;

import java.util.List;

public interface TransactionsRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findAllSortedByTimestampDesc();
}
