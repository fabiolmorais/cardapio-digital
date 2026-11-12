package br.com.cardapio.application.port.out.live;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

/** O movimento do restaurante agora, para o "Ao vivo", o mapa do salão e o Modo TV. */
public interface LiveFloorPort {

    /** As mesas ativas (de setores ativos), por setor e número, com celulares e comanda. */
    List<LiveTableView> findTables(EstablishmentId establishmentId, Instant now);

    /** O que as comandas fechadas hoje (no dia do restaurante) somaram. */
    Money revenueOn(EstablishmentId establishmentId, Instant now);

    /** A espera média, em segundos, dos chamados de hoje que alguém aceitou; vazio se nenhum. */
    OptionalLong averageWaitSeconds(EstablishmentId establishmentId, Instant now);
}
