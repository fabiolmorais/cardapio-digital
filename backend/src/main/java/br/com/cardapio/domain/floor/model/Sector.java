package br.com.cardapio.domain.floor.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/** Parte do salão (Salão principal, Deck, Varanda). As mesas ficam dentro de um setor. */
public record Sector(SectorId id, EstablishmentId establishmentId, String name, int position, boolean active) {

    public static final int NAME_MAX = 40;

    public Sector {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        name = validName(name);
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    /** Setor novo entra no fim da lista (position = quantos já existem). */
    public static Sector create(EstablishmentId establishmentId, String name, int position) {
        return new Sector(SectorId.newId(), establishmentId, name, position, true);
    }

    public Sector rename(String newName) {
        return new Sector(id, establishmentId, newName, position, active);
    }

    /** O registro fica (as mesas antigas e o histórico apontam para ele). */
    public Sector deactivate() {
        return new Sector(id, establishmentId, name, position, false);
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Dê um nome ao setor, como \"Salão principal\"");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX) {
            throw new InvalidValueException("Nome do setor longo demais (máximo " + NAME_MAX + " caracteres)");
        }
        return trimmed;
    }
}
