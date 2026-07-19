package com.teya.ledger.services;

import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.CreateTransactionResult;
import com.teya.ledger.dtos.TransactionResponse;
import com.teya.ledger.exceptions.IdempotencyKeyMismatchException;
import com.teya.ledger.exceptions.InsufficientFundsException;
import com.teya.ledger.exceptions.MissingIdempotencyKeyException;
import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.datamodels.IdempotencyDataModel;
import com.teya.ledger.persistence.TransactionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private TransactionsRepository transactionsRepository;
    private CacheRepository cacheRepository;
    private IdempotencyService idempotencyService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionsRepository = mock(TransactionsRepository.class);
        cacheRepository = mock(CacheRepository.class);
        idempotencyService = mock(IdempotencyService.class);
        transactionService = new TransactionService(transactionsRepository, cacheRepository, idempotencyService);
    }

    @Test
    void createTransaction_WithdrawalResultingInZeroBalance_SuccessfullyCreatesTransaction() {
        // Arrange
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String withdrawalDesc = "Withdrawal bringing balance to 0";
        Money moneyTen = new Money(new BigDecimal("10.00"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);
        UUID idempotencyKey = UUID.randomUUID();

        CreateTransactionCommand command = new CreateTransactionCommand(
                idempotencyKey, accId, withdrawalDesc, TransactionType.WITHDRAWAL, moneyTen, timestamp
        );

        // Mock idempotency service to indicate this is a NEW request
        when(idempotencyService.validateIdempotencyKey(command)).thenReturn(Optional.empty());
        when(cacheRepository.getCurrentBalance()).thenReturn(new BigDecimal("10.00"));
        when(transactionsRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateTransactionResult result = transactionService.createTransaction(command);

        // Assert
        assertNotNull(result);
        assertFalse(result.replayed());
        TransactionResponse response = result.response();
        assertNotNull(response.transactionId());
        assertEquals(accId, response.accountId());
        assertEquals(withdrawalDesc, response.description());
        assertEquals(TransactionType.WITHDRAWAL.name(), response.transactionType());
        assertEquals(new BigDecimal("10.00"), response.amount());
        assertEquals(timestamp, response.timestamp());
        assertEquals(new BigDecimal("0.00"), response.balanceAfter());

        verify(cacheRepository).setCurrentBalance(new BigDecimal("0.00"));
        verify(idempotencyService).save(eq(idempotencyKey), any(IdempotencyDataModel.class));
    }

    @Test
    void createTransaction_WithdrawalResultingInNegativeBalance_ThrowsInsufficientFundsException() {
        // Arrange
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String description = "Rent";
        Money moneyZeroPointOne = new Money(new BigDecimal("0.10"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);
        UUID idempotencyKey = UUID.randomUUID();

        CreateTransactionCommand command = new CreateTransactionCommand(
                idempotencyKey, accId, description, TransactionType.WITHDRAWAL, moneyZeroPointOne, timestamp
        );

        when(idempotencyService.validateIdempotencyKey(command)).thenReturn(Optional.empty());
        when(cacheRepository.getCurrentBalance()).thenReturn(BigDecimal.ZERO);

        // Act & Assert
        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> transactionService.createTransaction(command)
        );

        assertEquals("Insufficient funds for withdrawal", exception.getMessage());
        verify(transactionsRepository, never()).save(any(Transaction.class));
        verify(idempotencyService, never()).save(any(UUID.class), any(IdempotencyDataModel.class));
    }

    @Test
    void createTransaction_DepositTransaction_SuccessfullyCreatesTransaction() {
        // Arrange
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String description = "Simple Deposit";
        Money moneyTen = new Money(new BigDecimal("10.00"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);
        UUID idempotencyKey = UUID.randomUUID();

        CreateTransactionCommand command = new CreateTransactionCommand(
                idempotencyKey, accId, description, TransactionType.DEPOSIT, moneyTen, timestamp
        );

        when(idempotencyService.validateIdempotencyKey(command)).thenReturn(Optional.empty());
        when(cacheRepository.getCurrentBalance()).thenReturn(BigDecimal.ZERO);
        when(transactionsRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateTransactionResult result = transactionService.createTransaction(command);

        // Assert
        assertNotNull(result);
        assertFalse(result.replayed());
        assertEquals(new BigDecimal("10.00"), result.response().balanceAfter());
        verify(transactionsRepository).save(any(Transaction.class));
        verify(idempotencyService).save(eq(idempotencyKey), any(IdempotencyDataModel.class));
    }

    @Test
    void createTransaction_WhenNullIdempotencyKey_ThrowsMissingIdempotencyKeyException() {
        // Arrange
        CreateTransactionCommand command = new CreateTransactionCommand(
                null, UUID.randomUUID(), "Deposit", TransactionType.DEPOSIT,
                new Money(new BigDecimal("10.00")), Instant.now()
        );

        // Because TransactionService delegates to IdempotencyService, we mock the exception throw
        when(idempotencyService.validateIdempotencyKey(command))
                .thenThrow(new MissingIdempotencyKeyException("Idempotency-Key header is required"));

        // Act & Assert
        assertThrows(MissingIdempotencyKeyException.class, () -> transactionService.createTransaction(command));
        verify(transactionsRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WhenSameKeyAndMatchingPayload_ReplaysOriginalResponseAndDoesNotWriteAgain() {
        // Arrange
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String description = "Salary";
        Money moneyHundred = new Money(new BigDecimal("100.00"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);
        UUID idempotencyKey = UUID.randomUUID();

        CreateTransactionCommand command = new CreateTransactionCommand(
                idempotencyKey, accId, description, TransactionType.DEPOSIT, moneyHundred, timestamp
        );

        TransactionResponse originalResponse = new TransactionResponse(
                UUID.randomUUID(), accId, description, TransactionType.DEPOSIT.name(),
                new BigDecimal("100.00"), timestamp, new BigDecimal("100.00")
        );

        CreateTransactionResult replayedResult = new CreateTransactionResult(originalResponse, true);

        // Mock idempotency service returning an EXISTING result
        when(idempotencyService.validateIdempotencyKey(command)).thenReturn(Optional.of(replayedResult));

        // Act
        CreateTransactionResult result = transactionService.createTransaction(command);

        // Assert
        assertTrue(result.replayed());
        assertEquals(originalResponse, result.response());
        verify(transactionsRepository, never()).save(any(Transaction.class));
        verify(cacheRepository, never()).setCurrentBalance(any(BigDecimal.class));
        verify(idempotencyService, never()).save(any(UUID.class), any(IdempotencyDataModel.class));
    }

    @Test
    void createTransaction_WhenSameKeyButDifferentPayload_ThrowsIdempotencyKeyMismatchException() {
        // Arrange
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);
        UUID idempotencyKey = UUID.randomUUID();

        CreateTransactionCommand conflictingCommand = new CreateTransactionCommand(
                idempotencyKey, accId, "ATM", TransactionType.WITHDRAWAL,
                new Money(new BigDecimal("50.00")), timestamp
        );

        // Mock idempotency service throwing the mismatch exception
        when(idempotencyService.validateIdempotencyKey(conflictingCommand))
                .thenThrow(new IdempotencyKeyMismatchException("Idempotency key was already used for a different request payload"));

        // Act & Assert
        assertThrows(IdempotencyKeyMismatchException.class,
                () -> transactionService.createTransaction(conflictingCommand));

        verify(transactionsRepository, never()).save(any(Transaction.class));
        verify(idempotencyService, never()).save(any(UUID.class), any(IdempotencyDataModel.class));
    }

    @Test
    void createTransaction_GeneratesNewTransactionIdForEachFreshRequest() {
        // Arrange
        UUID accId = UUID.randomUUID();
        Money money = new Money(new BigDecimal("10.00"));
        Instant timestamp = Instant.now();

        when(cacheRepository.getCurrentBalance()).thenReturn(BigDecimal.ZERO);
        when(transactionsRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // FIXED: Different requests must have different idempotency keys!
        // If they have the same key, they are considered retries of the same request.
        CreateTransactionCommand first = new CreateTransactionCommand(
                UUID.randomUUID(), accId, "First", TransactionType.DEPOSIT, money, timestamp
        );
        CreateTransactionCommand second = new CreateTransactionCommand(
                UUID.randomUUID(), accId, "Second", TransactionType.DEPOSIT, money, timestamp
        );

        when(idempotencyService.validateIdempotencyKey(first)).thenReturn(Optional.empty());
        when(idempotencyService.validateIdempotencyKey(second)).thenReturn(Optional.empty());

        // Act
        CreateTransactionResult firstResult = transactionService.createTransaction(first);
        CreateTransactionResult secondResult = transactionService.createTransaction(second);

        // Assert
        assertNotEquals(firstResult.response().transactionId(), secondResult.response().transactionId());
    }

    @Test
    void getAllTransactions_WhenNoTransactions_ReturnsEmptyList() {
        // Arrange
        when(transactionsRepository.findAllSortedByTimestampDesc()).thenReturn(Collections.emptyList());

        // Act
        List<Transaction> actualResult = transactionService.getAllTransactions();

        // Assert
        assertTrue(actualResult.isEmpty());
    }

    @Test
    void getAllTransactions_WhenTransactionsExist_ReturnsTransactionsList() {
        // Arrange
        UUID txnId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID txnId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);

        Transaction deposit = new Transaction(
                txnId1, accId, "Deposit", new Money(new BigDecimal("15.00")),
                TransactionType.DEPOSIT, timestamp, new BigDecimal("15.00")
        );
        Transaction withdrawal = new Transaction(
                txnId2, accId, "Withdrawal", new Money(new BigDecimal("10.00")),
                TransactionType.WITHDRAWAL, timestamp, new BigDecimal("10.00")
        );
        List<Transaction> expectedList = List.of(deposit, withdrawal);

        when(transactionsRepository.findAllSortedByTimestampDesc()).thenReturn(expectedList);

        // Act
        List<Transaction> actualResult = transactionService.getAllTransactions();

        // Assert
        assertEquals(2, actualResult.size());
        assertEquals(TransactionType.DEPOSIT, actualResult.get(0).getTransactionType());
        assertEquals(TransactionType.WITHDRAWAL, actualResult.get(1).getTransactionType());
        assertEquals(expectedList, actualResult);
    }
}
