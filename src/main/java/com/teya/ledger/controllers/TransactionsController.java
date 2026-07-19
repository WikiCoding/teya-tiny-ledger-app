package com.teya.ledger.controllers;

import com.teya.ledger.dtos.CreateTransactionResult;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionsController {
    private final TransactionService transactionService;

    public TransactionsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody TransactionRequest transactionRequest) {
        CreateTransactionResult result = transactionService.createTransaction(
                new CreateTransactionCommand(
                        idempotencyKey,
                        transactionRequest.accountId(),
                        transactionRequest.description(),
                        TransactionType.valueOf(transactionRequest.transactionType().trim().toUpperCase()),
                        new Money(transactionRequest.amount()),
                        transactionRequest.timestamp()
                )
        );

        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.response());
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
