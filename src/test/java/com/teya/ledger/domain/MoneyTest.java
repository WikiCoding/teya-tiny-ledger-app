package com.teya.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MoneyTest {
    @Test
    void constructor_WithValidPositiveGetAmount_SuccessfullyCreatesInstance() {
        // Arrange
        BigDecimal validAmountValue = new BigDecimal("100.50");

        // Act
        Money money = new Money(validAmountValue);

        // Assert
        assertNotNull(money);
        assertEquals(validAmountValue, money.getAmount());
    }

    @Test
    void constructor_WithZeroGetAmount_ThrowsIllegalArgumentException() {
        // Arrange
        BigDecimal zeroValue = BigDecimal.ZERO;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Money(zeroValue)
        );

        assertEquals("Amount cannot be negative or zero", exception.getMessage());
    }


    @Test
    void constructor_WithNullGetAmount_ThrowsIllegalArgumentException() {
        // Arrange
        BigDecimal nullValue = null;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Money(nullValue)
        );

        assertEquals("Amount cannot be null", exception.getMessage());
    }

    @Test
    void constructor_WithNegativeGetAmount_ThrowsIllegalArgumentException() {
        // Arrange
        BigDecimal negativeValue = new BigDecimal("-0.01");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Money(negativeValue)
        );

        assertEquals("Amount cannot be negative or zero", exception.getMessage());
    }
}
