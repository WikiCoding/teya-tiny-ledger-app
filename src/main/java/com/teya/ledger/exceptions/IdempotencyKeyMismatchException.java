package com.teya.ledger.exceptions;

public class IdempotencyKeyMismatchException extends RuntimeException {
    public IdempotencyKeyMismatchException(String message) {
        super(message);
    }
}
