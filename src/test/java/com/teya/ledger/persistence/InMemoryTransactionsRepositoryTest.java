package com.teya.ledger.persistence;

import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.exceptions.DuplicateTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTransactionsRepositoryTest {

    private InMemoryTransactionsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionsRepository();
    }

    @Test
    void save_NewTransaction_SuccessfullySavesAndReturnsSameTransaction() {
        // Arrange
        Transaction transaction = createDummyTransaction(
                UUID.randomUUID(),
                "Gas Station",
                new BigDecimal("45.50"),
                Instant.now(),
                new BigDecimal("45.50")
        );

        // Act
        Transaction savedTransaction = repository.save(transaction);

        // Assert
        assertNotNull(savedTransaction);
        assertEquals(transaction, savedTransaction);
    }

    @Test
    void save_DuplicateTransaction_DoesNotOverWriteExistingEntry() {
        // Arrange
        UUID sameTxnUuid = UUID.randomUUID();
        Instant originalTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        Transaction originalTransaction = createDummyTransaction(
                sameTxnUuid,
                "Original Purchase",
                new BigDecimal("10.00"),
                originalTime,
                new BigDecimal("10.00")
        );
        Transaction duplicateTransaction = createDummyTransaction(
                sameTxnUuid,
                "Duplicate Attempt",
                new BigDecimal("20.00"),
                Instant.now(),
                new BigDecimal("20.00")
        );

        // Act
        repository.save(originalTransaction);

        // Assert
        assertThrows(DuplicateTransactionException.class, () -> repository.save(duplicateTransaction));
    }

    @Test
    void findAll_SortedByTimestampDesc_EmptyRepository_ReturnsEmptyList() {
        // Act
        List<Transaction> result = repository.findAllSortedByTimestampDesc();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_SortedByTimestampDesc_MultipleTransactions_ReturnsThemSortedByTimestampDescending() {
        // Arrange
        Instant baseTime = Instant.now();
        Instant oldestTime = baseTime.minus(2, ChronoUnit.HOURS);
        Instant middleTime = baseTime.minus(1, ChronoUnit.HOURS);

        Transaction oldestTxn = createDummyTransaction(UUID.randomUUID(), "Oldest", new BigDecimal("5.00"), oldestTime, new BigDecimal("5.00"));
        Transaction middleTxn = createDummyTransaction(UUID.randomUUID(), "Middle", new BigDecimal("10.00"), middleTime, new BigDecimal("10.00"));
        Transaction newestTxn = createDummyTransaction(UUID.randomUUID(), "Newest", new BigDecimal("15.00"), baseTime, new BigDecimal("15.00"));

        repository.save(middleTxn);
        repository.save(oldestTxn);
        repository.save(newestTxn);

        // Act
        List<Transaction> result = repository.findAllSortedByTimestampDesc();

        // Assert
        assertEquals(3, result.size());

        // Assert the sorting order is descending (newest first)
        assertEquals("Newest", result.get(0).getDescription());
        assertEquals("Middle", result.get(1).getDescription());
        assertEquals("Oldest", result.get(2).getDescription());
    }

    private Transaction createDummyTransaction(UUID txnUuid,
                                               String descriptionText,
                                               BigDecimal amountValue,
                                               Instant instant,
                                               BigDecimal balanceAfter) {
        return new Transaction(
                txnUuid,
                UUID.randomUUID(),
                descriptionText,
                new Money(amountValue),
                TransactionType.DEPOSIT,
                instant,
                balanceAfter
        );
    }
}