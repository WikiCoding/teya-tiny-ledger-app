package com.teya.ledger.exceptions;

import com.teya.ledger.dtos.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = InsufficientFundsException.class)
    public ResponseEntity<ErrorDto> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorDto(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(value = DuplicateTransactionException.class)
    public ResponseEntity<ErrorDto> handleDuplicateTransaction(DuplicateTransactionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(value = MissingIdempotencyKeyException.class)
    public ResponseEntity<ErrorDto> handleMissingIdempotencyKey(MissingIdempotencyKeyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(value = IdempotencyKeyMismatchException.class)
    public ResponseEntity<ErrorDto> handleIdempotencyKeyMismatch(IdempotencyKeyMismatchException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorDto(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorDto> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ex.getMessage(), Instant.now()));
    }
}
