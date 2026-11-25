package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.HourPeak;
import br.com.cardapio.domain.report.model.KitchenItem;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os picos da cozinha (REL-34) com o cálculo novo (C9, ADR 0014): o mesmo resultado da versão com
 * objetos, inclusive nos empates, no item pronto "antes" de chegar e na virada do horário de verão.
 */
class KitchenPeaksTest {

    private static final ProductId PRATO = new ProductId(UUID.randomUUID());
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static KitchenItem item(Instant received, Instant ready) {
        return new KitchenItem(PRATO, "Prato", "Pratos", received, ready, null);
    }

    /** A versão de antes do C9, como referência. */
    private static List<HourPeak> reference(Collection<KitchenItem> items, ZoneId zone) {
        record Event(Instant at, int delta) {
        }
        List<Event> events = new ArrayList<>();
        for (KitchenItem i : items) {
            if (i.ready() != null) {
                events.add(new Event(i.received(), 1));
                events.add(new Event(i.ready().isBefore(i.received()) ? i.received() : i.ready(), -1));
            }
        }
        events.sort(Comparator.comparing(Event::at).thenComparingInt(Event::delta));
        Map<LocalDate, Map<Integer, Integer>> peaks = new TreeMap<>();
        int preparing = 0;
        for (Event e : events) {
            preparing += e.delta();
            var local = e.at().atZone(zone);
            peaks.computeIfAbsent(local.toLocalDate(), d -> new TreeMap<>())
                    .merge(local.getHour(), preparing, Math::max);
        }
        List<HourPeak> result = new ArrayList<>();
        peaks.forEach((day, hours) -> hours.forEach((hour, peak) -> {
            if (peak > 0) {
                result.add(new HourPeak(day, hour, peak));
            }
        }));
        return result;
    }

    @Test
    void sameAsBeforeWithTiesAndItemsReadyBeforeArriving() {
        Instant eight = Instant.parse("2026-11-20T23:00:00Z"); // 20h em São Paulo
        List<KitchenItem> items = List.of(
                item(eight, eight.plusSeconds(600)),
                // Sai no mesmo instante em que o próximo entra: a saída conta antes.
                item(eight.plusSeconds(600), eight.plusSeconds(1200)),
                item(eight.plusSeconds(300), eight.plusSeconds(100)), // pronto "antes" de chegar
                item(eight.plusSeconds(3500), null)); // ainda não ficou pronto: fica de fora
        assertThat(KitchenAnalysis.peaks(items, SAO_PAULO)).isEqualTo(reference(items, SAO_PAULO))
                .containsExactly(new HourPeak(LocalDate.parse("2026-11-20"), 20, 1));
    }

    @Test
    void sameAsBeforeOnRandomDaysAcrossDaylightSaving() {
        Random random = new Random(20261125);
        // 2018 teve horário de verão em São Paulo (novembro a fevereiro): as horas locais se repetem e pulam.
        Instant start = Instant.parse("2018-10-25T12:00:00Z");
        List<KitchenItem> items = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            Instant received = start.plusSeconds(random.nextInt(150 * 24 * 3600)).plusNanos(random.nextInt(3) * 500);
            Instant ready = random.nextInt(20) == 0 ? null
                    : received.plus(Duration.ofSeconds(random.nextInt(3600) - 60));
            items.add(item(received, ready));
        }
        for (ZoneId zone : List.of(SAO_PAULO, ZoneId.of("America/Manaus"), ZoneId.of("America/Noronha"))) {
            assertThat(KitchenAnalysis.peaks(items, zone)).isEqualTo(reference(items, zone));
        }
    }
}
