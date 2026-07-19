package com.teya.ledger.dtos;

import java.time.Instant;

public final class ErrorDto {
    private final String message;
    private final Instant timestamp;

    public ErrorDto(String message) {
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
