package com.teya.ledger;

import com.teya.ledger.domain.Transaction;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.persistence.CacheRepository;
import com.teya.ledger.persistence.TransactionsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class LedgerApplication {

    static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(TransactionsRepository transactionsRepository,
                                               CacheRepository cacheRepository) {
        return _ -> {
            List<Transaction> transactions = transactionsRepository.findAllSortedByTimestampDesc();

            BigDecimal currentBalance = calculateCurrentBalance(transactions);

            cacheRepository.setCurrentBalance(currentBalance);
        };
    }

    private BigDecimal calculateCurrentBalance(List<Transaction> transactions) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == TransactionType.DEPOSIT) {
                balance = balance.add(transaction.getMoney().getAmount());
            } else {
                balance = balance.subtract(transaction.getMoney().getAmount());
            }
        }
        return balance;
    }

}
