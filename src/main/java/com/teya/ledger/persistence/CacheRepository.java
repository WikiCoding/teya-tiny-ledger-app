package com.teya.ledger.persistence;

import java.math.BigDecimal;

public interface CacheRepository {
    void setCurrentBalance(BigDecimal currentBalance);
    BigDecimal getCurrentBalance();
}
