package com.teya.ledger.config;

import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.InMemoryCacheRepository;
import com.teya.ledger.persistence.TransactionsRepository;
import com.teya.ledger.persistence.InMemoryTransactionsRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerConfiguration {
    @Bean
    @ConditionalOnProperty(name = "ledger.transactions.repository", havingValue = "in-memory")
    public TransactionsRepository transactionsInMemoryRepository() {
        return new InMemoryTransactionsRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "ledger.balance.cache.repository", havingValue = "in-memory")
    public CacheRepository cacheRepository() {
        return new InMemoryCacheRepository();
    }
}
