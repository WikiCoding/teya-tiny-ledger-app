package com.teya.ledger.services;

import com.teya.ledger.persistence.CacheRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {
    private final CacheRepository cacheRepository;

    public BalanceService(CacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    public BigDecimal getLedgerBalance() {
        return cacheRepository.getCurrentBalance();
    }
}
