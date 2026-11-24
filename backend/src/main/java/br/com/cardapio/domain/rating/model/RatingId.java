package br.com.cardapio.domain.rating.model;

import java.util.Objects;
import java.util.UUID;

public record RatingId(UUID value) {

    public RatingId {
        Objects.requireNonNull(value);
    }

    public static RatingId newId() {
        return new RatingId(UUID.randomUUID());
    }
}
