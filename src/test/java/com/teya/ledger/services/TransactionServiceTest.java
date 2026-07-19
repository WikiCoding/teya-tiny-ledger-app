package com.teya.ledger.services;

import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.TransactionsRepository;
import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class TransactionServiceTest {

    private TransactionsRepository transactionsRepository;
    private CacheRepository cacheRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionsRepository = mock(TransactionsRepository.class);
        cacheRepository = mock(CacheRepository.class);
        transactionService = new TransactionService(transactionsRepository, cacheRepository);
    }

    @Test
    void createTransaction_WithdrawalResultingInZeroBalance_SuccessfullyCreatesTransaction() {
        // Arrange
        UUID txnId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID txnId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String withdrawalDesc = "Withdrawal bringing balance to 0";
        String depositDesc = "Initial Deposit";
        Money moneyTen = new Money(new BigDecimal("10.00"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);
        BigDecimal balanceAfter = new BigDecimal("10.00");

        CreateTransactionCommand command = new CreateTransactionCommand(
                txnId1,
                accId,
                withdrawalDesc,
                TransactionType.WITHDRAWAL,
                moneyTen,
                timestamp
        );

        Transaction existingDeposit = new Transaction(
                txnId2,
                accId,
                depositDesc,
                moneyTen,
                TransactionType.DEPOSIT,
                timestamp,
                balanceAfter
        );

        Transaction expectedSavedTransaction = new Transaction(
                command.transactionId(),
                command.accountId(),
                command.description(),
                command.money(),
                command.transactionType(),
                command.timestamp(),
                new BigDecimal("0.00")
        );

        when(cacheRepository.getCurrentBalance()).thenReturn(new BigDecimal("10.00"));
        when(transactionsRepository.findAllSortedByTimestampDesc()).thenReturn(List.of(existingDeposit));

        TransactionService service = new TransactionService(transactionsRepository, cacheRepository);

        when(transactionsRepository.save(expectedSavedTransaction)).thenReturn(expectedSavedTransaction);

        // Act
        Transaction actualResult = service.createTransaction(command);

        // Assert
        assertNotNull(actualResult);
        assertEquals(expectedSavedTransaction, actualResult);
        verify(transactionsRepository).save(expectedSavedTransaction);
    }

    @Test
    void createTransaction_WithdrawalResultingInNegativeBalance_ThrowsInsufficientFundsException() {
        // Arrange
        UUID txnId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        String description = "Rent";
        Money moneyZeroPointOne = new Money(new BigDecimal("0.10"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                txnId,
                accId,
                description,
                TransactionType.WITHDRAWAL,
                moneyZeroPointOne,
                timestamp
        );

        when(cacheRepository.getCurrentBalance()).thenReturn(BigDecimal.ZERO);
        when(transactionsRepository.findAllSortedByTimestampDesc()).thenReturn(Collections.emptyList());

        // Act & Assert
        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> transactionService.createTransaction(command)
        );

        assertEquals("Insufficient funds for withdrawal", exception.getMessage());
        verify(transactionsRepository, never()).save(null);
    }

    @Test
    void createTransaction_DepositTransaction_SuccessfullyCreatesTransaction() {
        // Arrange
        UUID txnId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID accId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String description = "Simple Deposit";
        Money moneyTen = new Money(new BigDecimal("10.00"));
        Instant timestamp = Instant.ofEpochMilli(1700000000000L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                txnId,
                accId,
                description,
                TransactionType.DEPOSIT,
                moneyTen,
                timestamp
        );

        Transaction expectedSavedTransaction = new Transaction(
                command.transactionId(),
                command.accountId(),
                command.description(),
                command.money(),
                command.transactionType(),
                command.timestamp(),
                new BigDecimal("10.00")
        );

        when(cacheRepository.getCurrentBalance()).thenReturn(BigDecimal.ZERO);
        when(transactionsRepository.save(expectedSavedTransaction)).thenReturn(expectedSavedTransaction);

        // Act
        Transaction actualResult = transactionService.createTransaction(command);

        // Assert
        assertNotNull(actualResult);
        assertEquals(expectedSavedTransaction, actualResult);
        verify(transactionsRepository).save(expectedSavedTransaction);
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
                txnId1,
                accId,
                "Deposit",
                new Money(new BigDecimal("15.00")),
                TransactionType.DEPOSIT,
                timestamp,
                new BigDecimal("15.00")
        );
        Transaction withdrawal = new Transaction(
                txnId2,
                accId,
                "Withdrawal",
                new Money(new BigDecimal("10.00")),
                TransactionType.WITHDRAWAL,
                timestamp,
                new BigDecimal("10.00")
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