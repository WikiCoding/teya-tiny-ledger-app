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

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LedgerEndToEndTest {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

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
        UUID accUuid = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Instant timestamp = Instant.parse("2025-07-15T16:24:00Z");
        String description = "Salary";
        String transactionType = "DEPOSIT";

        TransactionRequest requestPayload = new TransactionRequest(
                accUuid, description, transactionType, amount, timestamp
        );

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestPayload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.transactionId").isNotEmpty()
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
            UUID accId,
            String description,
            String type,
            BigDecimal amount,
            Instant timestamp
    ) {
        // Arrange
        TransactionRequest payload = new TransactionRequest(accId, description, type, amount, timestamp);

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<Arguments> provideInvalidTransactionData() {
        UUID validUuid = UUID.randomUUID();
        return Stream.of(
                // 1. Invalid AccountId (null)
                Arguments.of("Null AccountId", null, "Valid Desc", "DEPOSIT", BigDecimal.TEN, Instant.now()),

                // 2. Invalid Description (null, empty, too long)
                Arguments.of("Null Description", validUuid, null, "DEPOSIT", BigDecimal.TEN, Instant.now()),
                Arguments.of("Empty Description", validUuid, "   ", "DEPOSIT", BigDecimal.TEN, Instant.now()),
                Arguments.of("Description Too Long", validUuid, "A".repeat(36), "DEPOSIT", BigDecimal.TEN, Instant.now()),

                // 3. Invalid Amount (null, negative)
                Arguments.of("Null Amount", validUuid, "Valid Desc", "DEPOSIT", null, Instant.now()),
                Arguments.of("Negative Amount", validUuid, "Valid Desc", "DEPOSIT", new BigDecimal("-1.00"), Instant.now()),

                // 4. Invalid Timestamp (null, future)
                Arguments.of("Null Timestamp", validUuid, "Valid Desc", "DEPOSIT", BigDecimal.TEN, null),
                Arguments.of("Future Timestamp", validUuid, "Valid Desc", "DEPOSIT", BigDecimal.TEN, Instant.now().plusSeconds(3600)),

                // 5. Invalid TransactionType string
                Arguments.of("Invalid TransactionType", validUuid, "Valid Desc", "NOT_A_TYPE", BigDecimal.TEN, Instant.now())
        );
    }

    @Test
    void createTransaction_WhenIdempotencyKeyHeaderIsMissing_Returns400BadRequest() {
        // Arrange
        TransactionRequest payload = new TransactionRequest(
                UUID.randomUUID(), "Salary", "DEPOSIT", BigDecimal.TEN, Instant.now()
        );

        // Act & Assert (no Idempotency-Key header sent)
        webTestClient.post()
                .uri("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createTransaction_WhenSameIdempotencyKeyAndMatchingPayload_ReplaysOriginalResponseWith200Ok() {
        // Arrange
        UUID accUuid = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Instant timestamp = Instant.parse("2025-07-15T16:24:00Z");
        String description = "Salary";
        String transactionType = "DEPOSIT";
        UUID idempotencyKey = UUID.randomUUID();

        TransactionRequest payload = new TransactionRequest(
                accUuid, description, transactionType, amount, timestamp
        );

        // Act & Assert
        byte[] firstResponseBody = webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, String.valueOf(idempotencyKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.transactionId").isNotEmpty()
                .returnResult()
                .getResponseBodyContent();

        webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, String.valueOf(idempotencyKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.transactionId").isNotEmpty()
                .jsonPath("$.accountId").isEqualTo(accUuid.toString())
                .jsonPath("$.description").isEqualTo(description)
                .jsonPath("$.transactionType").isEqualTo(transactionType)
                .jsonPath("$.amount").isEqualTo(150.00)
                .jsonPath("$.timestamp").isEqualTo("2025-07-15T16:24:00Z")
                .jsonPath("$.balanceAfter").isEqualTo(150.00);

        // The history must contain exactly one transaction (no duplicate writes)
        webTestClient.get()
                .uri("/api/v1/transactions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);

        assertNotNull(firstResponseBody);
    }

    @Test
    void createTransaction_WhenSameIdempotencyKeyButDifferentPayload_Returns422UnprocessableEntity() {
        // Arrange
        UUID accUuid = UUID.randomUUID();
        String idempotencyKey = "ae6f8799-c820-425e-971a-a46b56a2cd70";

        TransactionRequest originalPayload = new TransactionRequest(
                accUuid, "Salary", "DEPOSIT", new BigDecimal("100.00"), Instant.parse("2025-07-15T16:24:00Z")
        );

        TransactionRequest conflictingPayload = new TransactionRequest(
                accUuid, "ATM", "WITHDRAWAL", new BigDecimal("50.00"), Instant.parse("2025-07-15T16:24:00Z")
        );

        // First call succeeds
        webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(originalPayload)
                .exchange()
                .expectStatus().isCreated();

        // Act & Assert — same key but different payload must be rejected
        webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(conflictingPayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void createTransaction_WhenWithdrawalExceedsCurrentBalance_Returns422UnprocessableEntity() {
        // Arrange
        TransactionRequest expensivePayload = new TransactionRequest(
                UUID.randomUUID(),
                "Expensive Dinner",
                "WITHDRAWAL",
                new BigDecimal("1000.00"),
                Instant.now()
        );

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/transactions")
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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
        UUID accUuid = UUID.randomUUID();
        Instant timestamp = Instant.parse("2025-07-15T16:00:00Z");

        CreateTransactionCommand transaction = new CreateTransactionCommand(
                UUID.randomUUID(),
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
                .jsonPath("$[0].transactionId").isNotEmpty()
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
