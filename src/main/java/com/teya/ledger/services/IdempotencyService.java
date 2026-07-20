package com.teya.ledger.services;

import com.teya.ledger.dtos.CreateTransactionCommand;
import com.teya.ledger.dtos.CreateTransactionResult;
import com.teya.ledger.exceptions.DuplicateTransactionException;
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

    public Optional<CreateTransactionResult> reserveOrGetExistingResult(CreateTransactionCommand command) {
        UUID idempotencyKey = command.idempotencyKey();
        if (idempotencyKey == null) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required");
        }

        Optional<IdempotencyDataModel> alreadyReserved = idempotencyRepository.reserve(idempotencyKey,
                new IdempotencyDataModel(command, null));

        if (alreadyReserved.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyDataModel idempotencyDataModel = alreadyReserved.get();
        if (!idempotencyDataModel.command().equals(command)) {
            throw new IdempotencyKeyMismatchException("Idempotency key was already used for a different request payload");
        }

        if (idempotencyDataModel.response() == null) {
            throw new DuplicateTransactionException("This idempotency key was previously used and cannot be reused");
        }

        boolean isReplayed = true;
        return Optional.of(new CreateTransactionResult(idempotencyDataModel.response(), isReplayed));
    }

    public void save(UUID idempotencyKey, IdempotencyDataModel idempotencyDataModel) {
        idempotencyRepository.save(idempotencyKey, idempotencyDataModel);
    }
}
