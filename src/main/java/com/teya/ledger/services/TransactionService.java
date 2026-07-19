package com.teya.ledger.services;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.CreateTransactionResult;
import com.teya.ledger.dtos.TransactionResponse;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.datamodels.IdempotencyDataModel;
import com.teya.ledger.persistence.TransactionsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionsRepository transactionsRepository;
    private final CacheRepository cacheRepository;
    private final IdempotencyService idempotencyService;

    public TransactionService(TransactionsRepository transactionsRepository,
                              CacheRepository cacheRepository,
                              IdempotencyService idempotencyService) {
        this.transactionsRepository = transactionsRepository;
        this.cacheRepository = cacheRepository;
        this.idempotencyService = idempotencyService;
    }

    public synchronized CreateTransactionResult createTransaction(final CreateTransactionCommand command) {
        return idempotencyService.validateIdempotencyKey(command).orElseGet(() -> processNewTransaction(command));
    }

    private CreateTransactionResult processNewTransaction(CreateTransactionCommand command) {
        final BigDecimal currentBalance = cacheRepository.getCurrentBalance();
        final BigDecimal amount = command.money().getAmount();

        final BigDecimal balanceAfter = command.transactionType() == TransactionType.DEPOSIT
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        final Transaction transaction = new Transaction(
                UUID.randomUUID(),
                command.accountId(),
                command.description(),
                command.money(),
                command.transactionType(),
                command.timestamp(),
                balanceAfter
        );

        final Transaction saved = transactionsRepository.save(transaction);
        cacheRepository.setCurrentBalance(balanceAfter);

        final TransactionResponse response = toResponse(saved);
        idempotencyService.save(command.idempotencyKey(), new IdempotencyDataModel(command, response));

        return new CreateTransactionResult(response, false);
    }

    public List<Transaction> getAllTransactions() {
        return transactionsRepository.findAllSortedByTimestampDesc();
    }

    private static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getDescription(),
                transaction.getTransactionType().toString(),
                transaction.getMoney().getAmount(),
                transaction.getTimestamp(),
                transaction.getBalanceAfter()
        );
    }
}
