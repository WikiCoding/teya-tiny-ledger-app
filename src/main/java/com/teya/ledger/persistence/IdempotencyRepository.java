package com.teya.ledger.persistence;

import com.teya.ledger.persistence.datamodels.IdempotencyDataModel;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {
    Optional<IdempotencyDataModel> reserve(UUID idempotencyKey, IdempotencyDataModel placeholderRequest);
    void save(UUID idempotencyKey, IdempotencyDataModel request);
}
