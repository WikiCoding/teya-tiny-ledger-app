package com.teya.ledger.persistence;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotencyRepository implements IdempotencyRepository {
    private final ConcurrentMap<UUID, IdempotentRequest> requestsByKey;

    public InMemoryIdempotencyRepository() {
        this.requestsByKey = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<IdempotentRequest> findByKey(UUID idempotencyKey) {
        return Optional.ofNullable(requestsByKey.get(idempotencyKey));
    }

    @Override
    public void save(UUID idempotencyKey, IdempotentRequest request) {
        requestsByKey.put(idempotencyKey, request);
    }
}
