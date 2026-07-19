package com.teya.ledger.services;

import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.CreateTransactionResult;
import com.teya.ledger.dtos.TransactionResponse;
import com.teya.ledger.exceptions.IdempotencyKeyMismatchException;
import com.teya.ledger.exceptions.MissingIdempotencyKeyException;
import com.teya.ledger.persistence.IdempotencyRepository;
import com.teya.ledger.persistence.datamodels.IdempotencyDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {
    private IdempotencyRepository idempotencyRepository;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyRepository = mock(IdempotencyRepository.class);
        idempotencyService = new IdempotencyService(idempotencyRepository);
    }

    @Test
    void validateIdempotencyKey_WithNullKey_ThrowsMissingIdempotencyKeyException() {
        // Arrange
        CreateTransactionCommand commandWithNullKey = new CreateTransactionCommand(
                null, // Null key
                UUID.randomUUID(),
                "Deposit",
                TransactionType.DEPOSIT,
                new Money(new BigDecimal("10.00")),
                Instant.now()
        );

        // Act & Assert
        MissingIdempotencyKeyException exception = assertThrows(
                MissingIdempotencyKeyException.class,
                () -> idempotencyService.validateIdempotencyKey(commandWithNullKey)
        );

        assertEquals("Idempotency-Key header is required", exception.getMessage());
        verify(idempotencyRepository, never()).findByKey(any());
    }

    @Test
    void validateIdempotencyKey_WhenKeyNotFound_ReturnsEmptyOptional() {
        // Arrange
        UUID key = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                key, UUID.randomUUID(), "Deposit", TransactionType.DEPOSIT,
                new Money(new BigDecimal("10.00")), Instant.now()
        );

        when(idempotencyRepository.findByKey(key)).thenReturn(Optional.empty());

        // Act
        Optional<CreateTransactionResult> result = idempotencyService.validateIdempotencyKey(command);

        // Assert
        assertTrue(result.isEmpty());
        verify(idempotencyRepository).findByKey(key);
    }

    @Test
    void validateIdempotencyKey_WhenKeyExistsAndPayloadMatches_ReturnsReplayedResult() {
        // Arrange
        UUID key = UUID.randomUUID();
        UUID accId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        BigDecimal amount = new BigDecimal("100.00");

        CreateTransactionCommand command = new CreateTransactionCommand(
                key, accId, "Salary", TransactionType.DEPOSIT, new Money(amount), timestamp
        );

        TransactionResponse storedResponse = new TransactionResponse(
                UUID.randomUUID(), accId, "Salary", TransactionType.DEPOSIT.name(),
                amount, timestamp, amount
        );

        IdempotencyDataModel storedRequest = new IdempotencyDataModel(command, storedResponse);

        when(idempotencyRepository.findByKey(key)).thenReturn(Optional.of(storedRequest));

        // Act
        Optional<CreateTransactionResult> result = idempotencyService.validateIdempotencyKey(command);

        // Assert
        assertTrue(result.isPresent());
        CreateTransactionResult transactionResult = result.get();
        assertTrue(transactionResult.replayed());
        assertEquals(storedResponse, transactionResult.response());
    }

    @Test
    void validateIdempotencyKey_WhenKeyExistsButPayloadDiffers_ThrowsIdempotencyKeyMismatchException() {
        // Arrange
        UUID key = UUID.randomUUID();
        UUID accId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        CreateTransactionCommand storedCommand = new CreateTransactionCommand(
                key, accId, "Salary", TransactionType.DEPOSIT,
                new Money(new BigDecimal("100.00")), timestamp
        );
        TransactionResponse storedResponse = new TransactionResponse(
                UUID.randomUUID(), accId, "Salary", TransactionType.DEPOSIT.name(),
                new BigDecimal("100.00"), timestamp, new BigDecimal("100.00")
        );
        IdempotencyDataModel storedRequest = new IdempotencyDataModel(storedCommand, storedResponse);

        CreateTransactionCommand conflictingCommand = new CreateTransactionCommand(
                key, accId, "ATM", TransactionType.WITHDRAWAL,
                new Money(new BigDecimal("50.00")), timestamp
        );

        when(idempotencyRepository.findByKey(key)).thenReturn(Optional.of(storedRequest));

        // Act & Assert
        IdempotencyKeyMismatchException exception = assertThrows(
                IdempotencyKeyMismatchException.class,
                () -> idempotencyService.validateIdempotencyKey(conflictingCommand)
        );

        assertEquals("Idempotency key was already used for a different request payload", exception.getMessage());
    }

    @Test
    void save_DelegatesToRepository() {
        // Arrange
        UUID key = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                key, UUID.randomUUID(), "Deposit", TransactionType.DEPOSIT,
                new Money(new BigDecimal("10.00")), Instant.now()
        );
        TransactionResponse response = new TransactionResponse(
                UUID.randomUUID(), command.accountId(), "Deposit", TransactionType.DEPOSIT.name(),
                new BigDecimal("10.00"), command.timestamp(), new BigDecimal("10.00")
        );
        IdempotencyDataModel requestToSave = new IdempotencyDataModel(command, response);

        // Act
        idempotencyService.save(key, requestToSave);

        // Assert
        verify(idempotencyRepository, times(1)).save(key, requestToSave);
    }
}