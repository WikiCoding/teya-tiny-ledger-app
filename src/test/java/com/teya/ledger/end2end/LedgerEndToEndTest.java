package com.teya.ledger.end2end;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.domain.Money;
import com.teya.ledger.domain.TransactionType;
import com.teya.ledger.dtos.TransactionRequest;
import com.teya.ledger.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LedgerEndToEndTest {

    private WebTestClient webTestClient;

    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8080")
                .build();
    }

    // =========================================================================
    // POST /api/v1/transactions E2E Tests
    // =========================================================================

    @Test
    void createTransaction_E2E_SavesToDatabaseAndReturns201() {
        // Arrange
        UUID txnUuid = UUID.randomUUID();
        UUID accUuid = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Instant timestamp = Instant.parse("2025-07-15T16:24:00Z");
        String description = "Salary";
        String transactionType = "DEPOSIT";

        TransactionRequest requestPayload = new TransactionRequest(
                txnUuid, accUuid, description, transactionType, amount, timestamp
        );

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestPayload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.transactionId").isEqualTo(txnUuid.toString())
                .jsonPath("$.accountId").isEqualTo(accUuid.toString())
                .jsonPath("$.description").isEqualTo(description)
                .jsonPath("$.transactionType").isEqualTo(transactionType)
                .jsonPath("$.amount").isEqualTo(150.00)
                .jsonPath("$.timestamp").isEqualTo("2025-07-15T16:24:00Z")
                .jsonPath("$.balanceAfter").isEqualTo(150.00);
    }

    @ParameterizedTest(name = "Should return 400 when: {0}")
    @MethodSource("provideInvalidTransactionData")
    void createTransaction_WithInvalidData_Returns400BadRequest(
            String testCaseName,
            UUID txnId,
            UUID accId,
            String description,
            String type,
            BigDecimal amount,
            Instant timestamp
    ) {
        // Arrange
        TransactionRequest payload = new TransactionRequest(txnId, accId, description, type, amount, timestamp);

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<Arguments> provideInvalidTransactionData() {
        UUID validUuid = UUID.randomUUID();
        return Stream.of(
                // 1. Invalid TransactionId (null)
                Arguments.of("Null TransactionId", null, validUuid, "Valid Desc", "DEPOSIT", BigDecimal.TEN, Instant.now()),

                // 2. Invalid AccountId (null)
                Arguments.of("Null AccountId", validUuid, null, "Valid Desc", "DEPOSIT", BigDecimal.TEN, Instant.now()),

                // 3. Invalid Description (null, empty, too long)
                Arguments.of("Null Description", validUuid, validUuid, null, "DEPOSIT", BigDecimal.TEN, Instant.now()),
                Arguments.of("Empty Description", validUuid, validUuid, "   ", "DEPOSIT", BigDecimal.TEN, Instant.now()),
                Arguments.of("Description Too Long", validUuid, validUuid, "A".repeat(36), "DEPOSIT", BigDecimal.TEN, Instant.now()),

                // 4. Invalid Amount (null, negative)
                Arguments.of("Null Amount", validUuid, validUuid, "Valid Desc", "DEPOSIT", null, Instant.now()),
                Arguments.of("Negative Amount", validUuid, validUuid, "Valid Desc", "DEPOSIT", new BigDecimal("-1.00"), Instant.now()),

                // 5. Invalid Timestamp (null, future)
                Arguments.of("Null Timestamp", validUuid, validUuid, "Valid Desc", "DEPOSIT", BigDecimal.TEN, null),
                Arguments.of("Future Timestamp", validUuid, validUuid, "Valid Desc", "DEPOSIT", BigDecimal.TEN, Instant.now().plusSeconds(3600)),

                // 6. Invalid TransactionType string
                Arguments.of("Invalid TransactionType", validUuid, validUuid, "Valid Desc", "NOT_A_TYPE", BigDecimal.TEN, Instant.now())
        );
    }

    @Test
    void createTransaction_WhenTransactionIdAlreadyExists_Returns409Conflict() {
        // Arrange
        UUID txnUuid = UUID.randomUUID();

        CreateTransactionCommand existingTransaction = new CreateTransactionCommand(
                txnUuid,
                UUID.randomUUID(),
                "Initial",
                TransactionType.DEPOSIT,
                new Money(BigDecimal.TEN),
                Instant.now()
        );

        transactionService.createTransaction(existingTransaction);

        // Act
        TransactionRequest duplicatePayload = new TransactionRequest(
                txnUuid, UUID.randomUUID(), "Duplicate Txn", "DEPOSIT", BigDecimal.TEN, Instant.now()
        );

        // Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(duplicatePayload)
                .exchange()
                .expectStatus().isEqualTo(409); // Conflict
    }

    @Test
    void createTransaction_WhenWithdrawalExceedsCurrentBalance_Returns422UnprocessableEntity() {
        // Arrange
        TransactionRequest expensivePayload = new TransactionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Expensive Dinner",
                "WITHDRAWAL",
                new BigDecimal("1000.00"),
                Instant.now()
        );

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(expensivePayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    // =========================================================================
    // GET /api/v1/transactions E2E Tests
    // =========================================================================

    @Test
    void getAllTransactions_ReturnsListOfTransactionsAnd200Ok() {
        // Arrange
        UUID txnUuid = UUID.randomUUID();
        UUID accUuid = UUID.randomUUID();
        Instant timestamp = Instant.parse("2025-07-15T16:00:00Z");

        CreateTransactionCommand transaction = new CreateTransactionCommand(
                txnUuid,
                accUuid,
                "Salary Payment",
                TransactionType.DEPOSIT,
                new Money(new BigDecimal("3000.00")),
                timestamp
        );

        transactionService.createTransaction(transaction);

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/transactions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].transactionId").isEqualTo(txnUuid.toString())
                .jsonPath("$[0].accountId").isEqualTo(accUuid.toString())
                .jsonPath("$[0].description").isEqualTo("Salary Payment")
                .jsonPath("$[0].transactionType").isEqualTo("DEPOSIT")
                .jsonPath("$[0].amount").isEqualTo(3000.00)
                .jsonPath("$[0].timestamp").isEqualTo("2025-07-15T16:00:00Z")
                .jsonPath("$[0].balanceAfter").isEqualTo(3000.00);
    }

    // =========================================================================
    // GET /api/v1/balances E2E Tests
    // =========================================================================

    @Test
    void getBalance_E2E_CalculatesRealBalanceFromDb() {
        // Arrange
        UUID accUuid = UUID.randomUUID();

        CreateTransactionCommand deposit = new CreateTransactionCommand(
                UUID.randomUUID(),
                accUuid,
                "Salary",
                TransactionType.DEPOSIT,
                new Money(new BigDecimal("1000.00")),
                Instant.now()
        );

        CreateTransactionCommand withdrawal = new CreateTransactionCommand(
                UUID.randomUUID(),
                accUuid,
                "Groceries",
                TransactionType.WITHDRAWAL,
                new Money(new BigDecimal("150.00")),
                Instant.now()
        );

        transactionService.createTransaction(deposit);
        transactionService.createTransaction(withdrawal);

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/balances")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(850.00);
    }
}
