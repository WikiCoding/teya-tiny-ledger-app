package com.teya.ledger.services;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.CreateTransactionResult;
import com.teya.ledger.exceptions.IdempotencyKeyMismatchException;
import com.teya.ledger.exceptions.MissingIdempotencyKeyException;
import com.teya.ledger.persistence.IdempotencyRepository;
import com.teya.ledger.persistence.datamodels.IdempotencyDataModel;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyService(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    public Optional<CreateTransactionResult> validateIdempotencyKey(CreateTransactionCommand command) {
        UUID idempotencyKey = command.idempotencyKey();
        if (idempotencyKey == null) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required");
        }

        final Optional<IdempotencyDataModel> existingIdempotentRequest = idempotencyRepository.findByKey(idempotencyKey);

        if (existingIdempotentRequest.isPresent()) {
            IdempotencyDataModel idempotencyDataModel = existingIdempotentRequest.get();
            if (!idempotencyDataModel.command().equals(command)) {
                throw new IdempotencyKeyMismatchException(
                        "Idempotency key was already used for a different request payload"
                );
            }
            return Optional.of(new CreateTransactionResult(idempotencyDataModel.response(), true));
        }

        return Optional.empty();
    }

    public void save(UUID idempotencyKey, IdempotencyDataModel idempotencyDataModel) {
        idempotencyRepository.save(idempotencyKey, idempotencyDataModel);
    }
}
