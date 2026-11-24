package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.application.port.in.report.PhoneRodizioReport;
import br.com.cardapio.domain.report.model.OriginPoint;
import br.com.cardapio.domain.report.model.PresenceCount;
import br.com.cardapio.domain.report.model.RodizioItem;
import br.com.cardapio.domain.report.model.TierCount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PhoneRodizioReport", description = "Pedido pelo celular e rodízio (Business)")
record PhoneRodizioReportResponse(
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED, description = "Como a série foi agrupada") Grouping grouping,
        @Schema(requiredMode = REQUIRED, description = "Quem lança os pedidos, em cada dia, semana ou mês")
        List<Origin> origin,
        @Schema(requiredMode = REQUIRED) Funnel funnel,
        @Schema(requiredMode = REQUIRED) Phones phones,
        @Schema(requiredMode = REQUIRED, description = "Como os celulares confirmaram a presença")
        List<Presence> presence,
        @Schema(requiredMode = REQUIRED) Rodizio rodizio,
        @Schema(requiredMode = REQUIRED, description = "As pessoas de cada faixa, da maior para a menor")
        List<Tier> tiers,
        @Schema(requiredMode = REQUIRED, description = "Os produtos mais pedidos no rodízio") List<Item> topItems,
        @Schema(requiredMode = REQUIRED) Card card,
        @Schema(requiredMode = REQUIRED, description = "Os cortes mais pedidos na passagem") List<Item> topCuts) {

    enum Grouping { DAY, WEEK, MONTH }

    enum Method { WIFI, GPS, STAFF }

    private static Long seconds(Optional<Duration> d) {
        return d.map(Duration::toSeconds).orElse(null);
    }

    @Schema(name = "ReportOrderOrigin", description = "Os pedidos enviados: pelo celular e pela equipe")
    record Origin(@Schema(requiredMode = REQUIRED) LocalDate start, @Schema(requiredMode = REQUIRED) LocalDate end,
                  @Schema(requiredMode = REQUIRED) int phone, @Schema(requiredMode = REQUIRED) int staff) {
        static Origin of(OriginPoint p) {
            return new Origin(p.start(), p.end(), p.phone(), p.staff());
        }
    }

    @Schema(name = "ReportPhoneFunnel", description = "Das comandas fechadas, quantas tiveram celular e pedido")
    record Funnel(@Schema(requiredMode = REQUIRED) int tabs, @Schema(requiredMode = REQUIRED) int scanned,
                  @Schema(requiredMode = REQUIRED) int phoneOrdered) {
    }

    @Schema(name = "ReportPhoneStats", description = "O uso do celular nas mesas")
    record Phones(@Schema(description = "Celulares por comanda com celular") Double phonesPerTab,
                  @Schema(description = "Desses, os que pediram") Double orderingPerTab,
                  @Schema(description = "Do QR ao primeiro pedido, em média") Long scanToOrderSeconds,
                  @Schema(requiredMode = REQUIRED, description = "Mesas (mesa e dia) que escanearam e não pediram")
                  int scannedNoOrder,
                  @Schema(requiredMode = REQUIRED) int rejected,
                  @Schema(requiredMode = REQUIRED) int blocked) {
    }

    @Schema(name = "ReportPresence", description = "Os celulares que confirmaram a presença de um jeito")
    record Presence(@Schema(requiredMode = REQUIRED) Method method, @Schema(requiredMode = REQUIRED) int sessions) {
        static Presence of(PresenceCount p) {
            return new Presence(Method.valueOf(p.method().name()), p.sessions());
        }
    }

    @Schema(name = "ReportRodizio", description = "O rodízio das comandas fechadas no período")
    record Rodizio(@Schema(requiredMode = REQUIRED) int tabs, @Schema(requiredMode = REQUIRED) int people,
                   @Schema(requiredMode = REQUIRED) long revenueCents,
                   @Schema(requiredMode = REQUIRED, description = "O resto dessas comandas") long extraCents,
                   @Schema(requiredMode = REQUIRED) int rounds, @Schema(requiredMode = REQUIRED) int items,
                   @Schema(description = "Do início ao fechamento, em média") Long timeSeconds,
                   @Schema(requiredMode = REQUIRED) int withLimit, @Schema(requiredMode = REQUIRED) int reachedLimit) {
    }

    @Schema(name = "ReportRodizioTier", description = "As pessoas de uma faixa")
    record Tier(@Schema(requiredMode = REQUIRED) String tier, @Schema(requiredMode = REQUIRED) int people) {
        static Tier of(TierCount t) {
            return new Tier(t.tier(), t.people());
        }
    }

    @Schema(name = "ReportRodizioItem", description = "Um produto do rodízio e as unidades pedidas")
    record Item(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) int quantity) {
        static Item of(RodizioItem i) {
            return new Item(i.name(), i.quantity());
        }
    }

    @Schema(name = "ReportPassagemCard", description = "O tempo dos cartões da passagem em cada cor")
    record Card(@Schema(requiredMode = REQUIRED) long servingSeconds,
                @Schema(requiredMode = REQUIRED) long pausedSeconds) {
    }

    static PhoneRodizioReportResponse from(PhoneRodizioReport r) {
        var f = r.funnel();
        var p = r.phones();
        var z = r.rodizio();
        return new PhoneRodizioReportResponse(Range.of(r.period()), Grouping.valueOf(r.grouping().name()),
                r.origin().stream().map(Origin::of).toList(),
                new Funnel(f.tabs(), f.scanned(), f.phoneOrdered()),
                new Phones(p.phonesPerTab().orElse(null), p.orderingPerTab().orElse(null),
                        seconds(p.scanToOrder()), p.scannedNoOrder(), p.rejected(), p.blocked()),
                r.presence().stream().map(Presence::of).toList(),
                new Rodizio(z.tabs(), z.people(), z.revenue().cents(), z.extra().cents(), z.rounds(), z.items(),
                        seconds(z.time()), z.withLimit(), z.reachedLimit()),
                r.tiers().stream().map(Tier::of).toList(),
                r.topItems().stream().map(Item::of).toList(),
                new Card(r.cardTime().serving().toSeconds(), r.cardTime().paused().toSeconds()),
                r.topCuts().stream().map(Item::of).toList());
    }
}
