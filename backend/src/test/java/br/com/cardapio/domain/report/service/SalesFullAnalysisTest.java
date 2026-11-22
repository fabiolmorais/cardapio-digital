package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CancelReasonTotal;
import br.com.cardapio.domain.report.model.PerPersonTicket;
import br.com.cardapio.domain.report.model.SectorSales;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SalesFullAnalysisTest {

    private static CancelReasonTotal reason(String reason, long cents) {
        return new CancelReasonTotal(reason, Money.ofCents(cents));
    }

    @Test
    void theThreeBiggestReasonsAndTheOthersTogether() {
        var reasons = List.of(reason("Cliente desistiu", 82000), reason("Acabou o ingrediente", 131000),
                reason("Pedido repetido", 39500), reason("Demorou", 12000), reason(null, 9000));

        assertThat(SalesFullAnalysis.topReasons(reasons)).containsExactly(reason("Acabou o ingrediente", 131000),
                reason("Cliente desistiu", 82000), reason("Pedido repetido", 39500), reason(null, 21000));
        assertThat(SalesFullAnalysis.total(reasons)).isEqualTo(Money.ofCents(273500));
        // Sem motivo nenhum: tudo em "Outros".
        assertThat(SalesFullAnalysis.topReasons(List.of(reason(null, 500)))).containsExactly(reason(null, 500));
        assertThat(SalesFullAnalysis.topReasons(List.of())).isEmpty();
    }

    @Test
    void perTablePerDayAndPerPerson() {
        var salao = new SectorSales(new SectorId(UUID.randomUUID()), "Salão", 12, Money.ofCents(15122000));
        assertThat(salao.perTablePerDay(30)).isEqualTo(Money.ofCents(42006));
        assertThat(new SectorSales(salao.sectorId(), "Vazio", 0, Money.ZERO).perTablePerDay(30)).isEqualTo(Money.ZERO);
        assertThat(new PerPersonTicket(Money.ofCents(10000), 3).average()).contains(Money.ofCents(3333));
        assertThat(PerPersonTicket.EMPTY.average()).isEmpty();
    }
}
