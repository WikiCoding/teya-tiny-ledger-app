package com.teya.ledger.persistence;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryCacheRepository implements CacheRepository {
    private final AtomicReference<BigDecimal> currentBalance;

    public InMemoryCacheRepository() {
        this.currentBalance = new AtomicReference<>(BigDecimal.ZERO);
    }

    @Override
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance.set(currentBalance);
    }

    @Override
    public BigDecimal getCurrentBalance() {
        return this.currentBalance.get();
    }
}
