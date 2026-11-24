package br.com.cardapio.domain.rating.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rating.exception.RatingStateException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A avaliação do atendimento (RF40): uma por celular da mesa, de 1 a 5 estrelas, com atalhos e um
 * comentário curto, ligada à comanda e ao garçom que mais atendeu.
 *
 * @param tabId           a comanda da visita; null se a mesa ainda não tinha comanda
 * @param waiterId        quem mais atendeu a mesa; null se ninguém da equipe atendeu
 * @param comment         null sem comentário
 * @param googleClickedAt quando o cliente tocou em "Avaliar no Google" (nota 4 ou 5); null se não tocou
 * @param resolvedAt      nota baixa: quando o gerente marcou o alerta como resolvido; null enquanto aberto
 */
public record ServiceRating(
        RatingId id,
        EstablishmentId establishmentId,
        TableSessionId sessionId,
        DiningTableId tableId,
        TabId tabId,
        UserId waiterId,
        int stars,
        Set<RatingTag> tags,
        String comment,
        Instant createdAt,
        Instant googleClickedAt,
        Instant resolvedAt,
        UserId resolvedBy
) {

    public static final int MAX_COMMENT = 280;
    /** Até esta nota, o gerente recebe o alerta na hora. */
    public static final int LOW = 2;
    /** Desta nota para cima, o cliente vê o convite ao Google. */
    public static final int HIGH = 4;

    public ServiceRating {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(createdAt);
        if (stars < 1 || stars > 5) {
            throw new InvalidValueException("A nota vai de 1 a 5 estrelas");
        }
        tags = tags == null || tags.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(tags));
        comment = comment == null || comment.isBlank() ? null : comment.strip();
        if (comment != null && comment.length() > MAX_COMMENT) {
            throw new InvalidValueException("O comentário vai até " + MAX_COMMENT + " letras");
        }
        if ((resolvedAt == null) != (resolvedBy == null)) {
            throw new IllegalArgumentException("Resolvido: quando e por quem, juntos");
        }
    }

    /** A mesa avaliou. */
    public static ServiceRating rate(EstablishmentId establishmentId, TableSessionId sessionId, DiningTableId tableId,
                                     TabId tabId, UserId waiterId, int stars, Set<RatingTag> tags, String comment,
                                     Instant now) {
        return new ServiceRating(RatingId.newId(), establishmentId, sessionId, tableId, tabId, waiterId, stars, tags,
                comment, now, null, null, null);
    }

    /** Nota 1 ou 2: avisa o gerente na hora. */
    public boolean isLow() {
        return stars <= LOW;
    }

    /** Nota 4 ou 5: o cliente vê o convite para avaliar no Google. */
    public boolean invitesToGoogle() {
        return stars >= HIGH;
    }

    /** O toque em "Avaliar no Google" (REL-84). Só com o convite, e conta uma vez. */
    public ServiceRating clickGoogle(Instant now) {
        if (!invitesToGoogle()) {
            throw new RatingStateException("O convite ao Google é só para nota 4 ou 5");
        }
        if (googleClickedAt != null) {
            return this;
        }
        return new ServiceRating(id, establishmentId, sessionId, tableId, tabId, waiterId, stars, tags, comment,
                createdAt, now, resolvedAt, resolvedBy);
    }

    /** O gerente cuidou da nota baixa: o alerta sai do painel. */
    public ServiceRating resolve(UserId by, Instant now) {
        Objects.requireNonNull(by);
        if (!isLow()) {
            throw new RatingStateException("Só nota baixa tem alerta para resolver");
        }
        if (resolvedAt != null) {
            return this;
        }
        return new ServiceRating(id, establishmentId, sessionId, tableId, tabId, waiterId, stars, tags, comment,
                createdAt, googleClickedAt, now, by);
    }
}
