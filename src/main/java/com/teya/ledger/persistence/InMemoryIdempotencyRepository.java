package com.teya.ledger.persistence;

import com.teya.ledger.persistence.datamodels.IdempotencyDataModel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotencyRepository implements IdempotencyRepository {
    private final ConcurrentMap<UUID, IdempotencyDataModel> requestsByKey;

    public InMemoryIdempotencyRepository() {
        this.requestsByKey = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<IdempotencyDataModel> findByKey(UUID idempotencyKey) {
        return Optional.ofNullable(requestsByKey.get(idempotencyKey));
    }

    @Override
    public void save(UUID idempotencyKey, IdempotencyDataModel request) {
        requestsByKey.put(idempotencyKey, request);
    }
}
