package com.teya.ledger.domain;

import com.teya.ledger.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {

    @Test
    void constructor_WithValidParameters_CreatesInstanceAndGettersReturnCorrectValues() {
        // Arrange
        UUID expectedTxnId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID expectedAccId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String expectedDescription = "Sallary Deposit";
        Money expectedMoney = new Money(new BigDecimal("1500.00"));
        TransactionType expectedType = TransactionType.DEPOSIT;
        Instant expectedTimestamp = Instant.ofEpochMilli(1700000000000L);
        BigDecimal balanceAfter = new BigDecimal("1500.00");

        // Act
        Transaction transaction = new Transaction(
                expectedTxnId,
                expectedAccId,
                expectedDescription,
                expectedMoney,
                expectedType,
                expectedTimestamp,
                balanceAfter
        );

        // Assert
        assertNotNull(transaction);
        assertEquals(expectedTxnId, transaction.getTransactionId());
        assertEquals(expectedAccId, transaction.getAccountId());
        assertEquals(expectedDescription, transaction.getDescription());
        assertEquals(expectedMoney, transaction.getMoney());
        assertEquals(expectedType, transaction.getTransactionType());
        assertEquals(expectedTimestamp, transaction.getTimestamp());
    }

    @ParameterizedTest
    @MethodSource("invalidTransactionArguments")
    void shouldRejectInvalidTransactionFields(
            UUID transactionId,
            UUID accountId,
            String description,
            Money money,
            TransactionType transactionType,
            Instant timestamp,
            BigDecimal balanceAfter,
            Class<? extends RuntimeException> expectedException,
            String expectedMessage
    ) {
        RuntimeException exception = assertThrows(expectedException, () -> new Transaction(
                transactionId,
                accountId,
                description,
                money,
                transactionType,
                timestamp,
                balanceAfter
        ));

        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Stream<Arguments> invalidTransactionArguments() {
        final UUID TRANSACTION_ID = UUID.randomUUID();
        final UUID ACCOUNT_ID = UUID.randomUUID();
        final String DESCRIPTION = "Valid transaction";
        final Money MONEY = new Money(BigDecimal.ONE);
        final TransactionType TRANSACTION_TYPE = TransactionType.DEPOSIT;
        final Instant TIMESTAMP = Instant.now();
        final BigDecimal BALANCE_AFTER = BigDecimal.TEN;

        return Stream.of(
                Arguments.of(
                        null,
                        ACCOUNT_ID,
                        DESCRIPTION,
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Transaction ID is required"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        null,
                        DESCRIPTION,
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Account ID cannot be null"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        null,
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Description is required"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        "",
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Description is required"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        "   ",
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Description is required"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        "123456789012345678901234567890123456",
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Description cannot exceed 35 characters"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        DESCRIPTION,
                        MONEY,
                        null,
                        TIMESTAMP,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Transaction type is required"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        DESCRIPTION,
                        MONEY,
                        TRANSACTION_TYPE,
                        null,
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Timestamp cannot be null"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        DESCRIPTION,
                        MONEY,
                        TRANSACTION_TYPE,
                        Instant.now().plusSeconds(60),
                        BALANCE_AFTER,
                        IllegalArgumentException.class,
                        "Timestamp cannot be in the future"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        DESCRIPTION,
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        BigDecimal.valueOf(-1),
                        InsufficientFundsException.class,
                        "Insufficient funds for withdrawal"
                ),
                Arguments.of(
                        TRANSACTION_ID,
                        ACCOUNT_ID,
                        DESCRIPTION,
                        MONEY,
                        TRANSACTION_TYPE,
                        TIMESTAMP,
                        null,
                        IllegalArgumentException.class,
                        "Balance after transaction cannot be null"
                )
        );
    }
}