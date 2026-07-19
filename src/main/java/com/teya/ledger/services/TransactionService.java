package com.teya.ledger.services;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.CreateTransactionResult;
import com.teya.ledger.dtos.TransactionResponse;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.exceptions.IdempotencyKeyMismatchException;
import com.teya.ledger.exceptions.MissingIdempotencyKeyException;
import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.IdempotencyRepository;
import com.teya.ledger.persistence.IdempotentRequest;
import com.teya.ledger.persistence.TransactionsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionsRepository transactionsRepository;
    private final CacheRepository cacheRepository;
    private final IdempotencyRepository idempotencyRepository;

    public TransactionService(TransactionsRepository transactionsRepository,
                              CacheRepository cacheRepository,
                              IdempotencyRepository idempotencyRepository) {
        this.transactionsRepository = transactionsRepository;
        this.cacheRepository = cacheRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    public synchronized CreateTransactionResult createTransaction(final CreateTransactionCommand command) {
        validateIdempotencyKey(command.idempotencyKey());

        final Optional<IdempotentRequest> existing = idempotencyRepository.findByKey(command.idempotencyKey());

        if (existing.isPresent()) {
            IdempotentRequest stored = existing.get();
            if (!stored.command().equals(command)) {
                throw new IdempotencyKeyMismatchException(
                        "Idempotency key was already used for a different request payload"
                );
            }
            return new CreateTransactionResult(stored.response(), true);
        }

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
        idempotencyRepository.save(
                command.idempotencyKey(),
                new IdempotentRequest(command, response)
        );

        return new CreateTransactionResult(response, false);
    }

    public List<Transaction> getAllTransactions() {
        return transactionsRepository.findAllSortedByTimestampDesc();
    }

    private static void validateIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required");
        }
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
