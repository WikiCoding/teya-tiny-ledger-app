package com.teya.ledger.controllers;

import com.teya.ledger.domain.Transaction;
import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.TransactionRequest;
import com.teya.ledger.dtos.TransactionResponse;
import com.teya.ledger.services.TransactionService;
import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionsController {
    private final TransactionService transactionService;

    public TransactionsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest transactionRequest) {
        Transaction transaction = transactionService.createTransaction(
                new CreateTransactionCommand(
                        transactionRequest.transactionId(),
                        transactionRequest.accountId(),
                        transactionRequest.description(),
                        TransactionType.valueOf(transactionRequest.transactionType().trim().toUpperCase()),
                        new Money(transactionRequest.amount()),
                        transactionRequest.timestamp()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getDescription(),
                transaction.getTransactionType().toString(),
                transaction.getMoney().getAmount(),
                transaction.getTimestamp(),
                transaction.getBalanceAfter()
        ));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions().stream().map(transaction ->
                new TransactionResponse(
                        transaction.getTransactionId(),
                        transaction.getAccountId(),
                        transaction.getDescription(),
                        transaction.getTransactionType().toString(),
                        transaction.getMoney().getAmount(),
                        transaction.getTimestamp(),
                        transaction.getBalanceAfter()
                )).toList());
    }
}
