package br.com.cardapio.domain.floor.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.time.Instant;
import java.util.Objects;

/**
 * Mesa do salão. O rótulo é o que vai impresso ("04", "Varanda 2"); o token é o que vai no QR.
 * Imutável: cada mudança devolve uma mesa nova.
 */
public record DiningTable(
        DiningTableId id,
        EstablishmentId establishmentId,
        SectorId sectorId,
        String label,
        TableToken token,
        Instant tokenIssuedAt,
        boolean active
) {

    public static final int LABEL_MAX = 10;

    public DiningTable {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(sectorId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(tokenIssuedAt);
        label = validLabel(label);
    }

    public static DiningTable create(EstablishmentId establishmentId, SectorId sectorId, String label,
                                     TableToken token, Instant now) {
        return new DiningTable(DiningTableId.newId(), establishmentId, sectorId, label, token, now, true);
    }

    public DiningTable relabel(String newLabel) {
        return new DiningTable(id, establishmentId, sectorId, newLabel, token, tokenIssuedAt, active);
    }

    public DiningTable moveTo(SectorId newSector) {
        return new DiningTable(id, establishmentId, Objects.requireNonNull(newSector), label, token, tokenIssuedAt, active);
    }

    public DiningTable activate() {
        return new DiningTable(id, establishmentId, sectorId, label, token, tokenIssuedAt, true);
    }

    public DiningTable deactivate() {
        return new DiningTable(id, establishmentId, sectorId, label, token, tokenIssuedAt, false);
    }

    /** "Trocar QR desta mesa": o token novo invalida o display impresso com o antigo. */
    public DiningTable rotateToken(TableToken newToken, Instant now) {
        return new DiningTable(id, establishmentId, sectorId, label, Objects.requireNonNull(newToken), now, active);
    }

    /** Letras, números, espaço e hífen: "04", "Varanda 2", "B-12". */
    private static String validLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new InvalidValueException("Dê um número ou nome à mesa, como \"04\"");
        }
        String trimmed = label.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > LABEL_MAX) {
            throw new InvalidValueException("Nome da mesa longo demais (máximo " + LABEL_MAX + " caracteres)");
        }
        if (!trimmed.matches("[\\p{L}\\p{N} -]+")) {
            throw new InvalidValueException("Use só letras, números, espaço e hífen no nome da mesa");
        }
        return trimmed;
    }
}
