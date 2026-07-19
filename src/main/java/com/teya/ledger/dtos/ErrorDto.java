package com.teya.ledger.dtos;

import java.time.Instant;

public record ErrorDto(String message, Instant timestamp) {}
