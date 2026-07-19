package com.teya.ledger.services;

import com.teya.ledger.persistence.CacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BalanceServiceTest {
    private CacheRepository cacheRepository;

    @BeforeEach
    void setUp() {
        cacheRepository = mock(CacheRepository.class);
    }

    @Test
    void getResultingBalance_WhenNoTransactions_ReturnsZero() {
        // Arrange
        when(cacheRepository.getCurrentBalance()).thenReturn(BigDecimal.ZERO);
        BalanceService balanceService = new BalanceService(cacheRepository);

        // Act
        BigDecimal actualBalance = balanceService.getLedgerBalance();

        // Assert
        assertEquals(BigDecimal.ZERO, actualBalance);
    }

    @Test
    void getResultingBalance_WithCacheHolderACurrentBalance_ReturnsCorrectBalance() {
        // Arrange
        when(cacheRepository.getCurrentBalance()).thenReturn(new BigDecimal("5.00"));

        BalanceService balanceService = new BalanceService(cacheRepository);

        // Act
        BigDecimal actualBalance = balanceService.getLedgerBalance();

        // Assert
        assertEquals(new BigDecimal("5.00"), actualBalance);
    }
}