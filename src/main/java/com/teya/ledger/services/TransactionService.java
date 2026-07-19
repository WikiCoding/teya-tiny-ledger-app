package com.teya.ledger.services;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.TransactionsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionsRepository transactionsRepository;
    private final CacheRepository cacheRepository;

    public TransactionService(TransactionsRepository transactionsRepository, CacheRepository cacheRepository) {
        this.transactionsRepository = transactionsRepository;
        this.cacheRepository = cacheRepository;
    }

    public synchronized Transaction createTransaction(final CreateTransactionCommand command) {
        final BigDecimal currentBalance = cacheRepository.getCurrentBalance();
        final BigDecimal amount = command.money().getAmount();

        final BigDecimal balanceAfter = command.transactionType() == TransactionType.DEPOSIT
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        final Transaction transaction = new Transaction(
                command.transactionId(),
                command.accountId(),
                command.description(),
                command.money(),
                command.transactionType(),
                command.timestamp(),
                balanceAfter
        );

        final Transaction saved = transactionsRepository.save(transaction);
        cacheRepository.setCurrentBalance(balanceAfter);

        return saved;
    }

    public List<Transaction> getAllTransactions() {
        return transactionsRepository.findAllSortedByTimestampDesc();
    }
}
