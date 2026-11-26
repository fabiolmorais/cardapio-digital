package br.com.cardapio.domain.floor.model;

import br.com.cardapio.domain.floor.exception.CounterNotAllowedException;
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
        boolean active,
        TableKind kind
) {

    public static final int LABEL_MAX = 10;

    /** O nome do balcão é mais comprido que o rótulo da mesa: "Balcão da varanda". */
    public static final int COUNTER_NAME_MAX = 30;

    public DiningTable {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(sectorId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(tokenIssuedAt);
        Objects.requireNonNull(kind);
        label = kind.isCounter() ? validCounterName(label) : validLabel(label);
    }

    public static DiningTable create(EstablishmentId establishmentId, SectorId sectorId, String label,
                                     TableToken token, Instant now) {
        return new DiningTable(DiningTableId.newId(), establishmentId, sectorId, label, token, now, true,
                TableKind.TABLE);
    }

    /**
     * Um balcão (ADR 0015). O token existe (a coluna é obrigatória e única), mas nunca é mostrado:
     * o balcão não tem QR Code, e o endereço público dele não abre.
     */
    public static DiningTable createCounter(EstablishmentId establishmentId, SectorId sectorId, String name,
                                            TableToken token, Instant now) {
        return new DiningTable(DiningTableId.newId(), establishmentId, sectorId, name, token, now, true,
                TableKind.COUNTER);
    }

    public boolean isCounter() {
        return kind.isCounter();
    }

    public DiningTable relabel(String newLabel) {
        return new DiningTable(id, establishmentId, sectorId, newLabel, token, tokenIssuedAt, active, kind);
    }

    public DiningTable moveTo(SectorId newSector) {
        return new DiningTable(id, establishmentId, Objects.requireNonNull(newSector), label, token, tokenIssuedAt, active,
                kind);
    }

    public DiningTable activate() {
        return new DiningTable(id, establishmentId, sectorId, label, token, tokenIssuedAt, true, kind);
    }

    public DiningTable deactivate() {
        return new DiningTable(id, establishmentId, sectorId, label, token, tokenIssuedAt, false, kind);
    }

    /** "Trocar QR desta mesa": o token novo invalida o display impresso com o antigo. O balcão não tem QR. */
    public DiningTable rotateToken(TableToken newToken, Instant now) {
        if (isCounter()) {
            throw new CounterNotAllowedException("O balcão não tem QR Code");
        }
        return new DiningTable(id, establishmentId, sectorId, label, Objects.requireNonNull(newToken), now, active,
                kind);
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

    /** Letras, números, espaço e hífen, até {@link #COUNTER_NAME_MAX}: "Balcão", "Balcão da varanda". */
    private static String validCounterName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Dê um nome ao balcão, como \"Balcão do bar\"");
        }
        String trimmed = name.trim().replaceAll("\\s+", " ");
        if (trimmed.length() > COUNTER_NAME_MAX) {
            throw new InvalidValueException("Nome do balcão longo demais (máximo " + COUNTER_NAME_MAX + " caracteres)");
        }
        if (!trimmed.matches("[\\p{L}\\p{N} -]+")) {
            throw new InvalidValueException("Use só letras, números, espaço e hífen no nome do balcão");
        }
        return trimmed;
    }
}
