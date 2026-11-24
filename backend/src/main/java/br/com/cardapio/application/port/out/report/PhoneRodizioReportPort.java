package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CardTrack;
import br.com.cardapio.domain.report.model.DailyOrigin;
import br.com.cardapio.domain.report.model.PhoneFunnel;
import br.com.cardapio.domain.report.model.PhoneStats;
import br.com.cardapio.domain.report.model.PresenceCount;
import br.com.cardapio.domain.report.model.RodizioItem;
import br.com.cardapio.domain.report.model.RodizioTotals;
import br.com.cardapio.domain.report.model.TierCount;
import br.com.cardapio.domain.rodizio.model.RodizioType;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * O pedido pelo celular e o rodízio (ADR 0013), no intervalo [início, fim). O setor é o da mesa. O
 * rodízio é o das comandas fechadas no período, como o faturamento.
 */
public interface PhoneRodizioReportPort {

    /** Os pedidos enviados de cada dia (no fuso), pelo celular e pela equipe; só os dias com pedido. */
    List<DailyOrigin> originByDay(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                  Optional<SectorId> sector);

    /** As comandas fechadas, as que tiveram celular na mesa e as que tiveram pedido pelo celular. */
    PhoneFunnel funnel(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** O uso do celular: celulares por comanda, do QR ao pedido, mesas sem pedido, recusas e bloqueios. */
    PhoneStats phones(EstablishmentId establishmentId, Instant start, Instant end, Instant now, ZoneId zone,
                      Optional<SectorId> sector);

    /** Como os celulares confirmaram a presença no período. */
    List<PresenceCount> presence(EstablishmentId establishmentId, Instant start, Instant end,
                                 Optional<SectorId> sector);

    /** O rodízio das comandas fechadas no período. */
    RodizioTotals rodizio(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** As pessoas de cada faixa, da maior para a menor. */
    List<TierCount> tiers(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector);

    /** Os produtos mais pedidos no rodízio (de um tipo, ou de todos), do mais pedido para o menos. */
    List<RodizioItem> topItems(EstablishmentId establishmentId, Instant start, Instant end,
                               Optional<SectorId> sector, Optional<RodizioType> type, int limit);

    /** Os cartões das mesas de passagem que começaram a partir de {@code since}, com as viradas. */
    List<CardTrack> cardTracks(EstablishmentId establishmentId, Instant start, Instant end, Instant since,
                               Optional<SectorId> sector);
}
