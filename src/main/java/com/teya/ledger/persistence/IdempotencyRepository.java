package com.teya.ledger.persistence;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {
    Optional<IdempotentRequest> findByKey(UUID idempotencyKey);

    void save(UUID idempotencyKey, IdempotentRequest request);
}
