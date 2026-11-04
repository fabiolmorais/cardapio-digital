package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.rodizio.model.GuestTier;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;

/**
 * O rodízio da mesa, como a equipe e o celular veem: o nome, as pessoas por faixa e o total.
 *
 * @param endsAt o fim previsto; null sem tempo limite
 * @param status o cartão verde/vermelho (no à la minute, sempre SERVING)
 */
public record TableRodizio(RodizioId rodizioId, String name, RodizioType type, List<GuestTier> guests, int people,
                           Money total, Instant startedAt, Instant endsAt, TabRodizioStatus status,
                           Instant statusChangedAt) {

    public TableRodizio {
        guests = List.copyOf(guests);
    }

    public static TableRodizio from(TabRodizio r) {
        return new TableRodizio(r.rodizioId(), r.rodizioName(), r.type(), r.guests(), r.people(), r.total(),
                r.startedAt(), r.endsAt(), r.status(), r.statusChangedAt());
    }
}
