package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.in.report.KitchenFull;
import br.com.cardapio.domain.report.model.CategoryPrep;
import br.com.cardapio.domain.report.model.DelayedSlot;
import br.com.cardapio.domain.report.model.HourTimes;
import br.com.cardapio.domain.report.model.PeakAverage;
import br.com.cardapio.domain.report.model.ServiceTimes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Os blocos completos da Cozinha e tempos (Business). */
final class KitchenFullDtos {

    private KitchenFullDtos() {
    }

    @Schema(name = "ReportServiceTimes", description = "O passe, o tempo até a mesa e as confirmações demoradas")
    record Times(@Schema(description = "De pronto até entregue, em média") Long passeSeconds,
                 @Schema(description = "Da chegada à cozinha até entregue, em média") Long toTableSeconds,
                 @Schema(requiredMode = REQUIRED, description = "Pedidos que esperaram o garçom confirmar mais de 5 min")
                 int lateConfirmations) {
        static Times of(ServiceTimes t) {
            return new Times(t.passe().map(Duration::toSeconds).orElse(null),
                    t.toTable().map(Duration::toSeconds).orElse(null), t.lateConfirmations());
        }
    }

    @Schema(name = "ReportHourTimes", description = "Os itens entregues que chegaram à cozinha numa hora do dia")
    record Hour(@Schema(requiredMode = REQUIRED) int hour, @Schema(requiredMode = REQUIRED) long prepSeconds,
                @Schema(requiredMode = REQUIRED) long passeSeconds, @Schema(requiredMode = REQUIRED) int items) {
        static Hour of(HourTimes h) {
            return new Hour(h.hour(), h.prep().toSeconds(), h.passe().toSeconds(), h.items());
        }
    }

    @Schema(name = "ReportPeak", description = "A média do pico de itens em preparo ao mesmo tempo numa hora")
    record Peak(@Schema(requiredMode = REQUIRED) int hour, @Schema(requiredMode = REQUIRED) BigDecimal average) {
        static Peak of(PeakAverage p) {
            return new Peak(p.hour(), p.average());
        }
    }

    @Schema(name = "ReportCategoryPrep", description = "O preparo médio de uma categoria")
    record Category(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) int preparations,
                    @Schema(requiredMode = REQUIRED) long averageSeconds) {
        static Category of(CategoryPrep c) {
            return new Category(c.category(), c.preparations(), c.average().toSeconds());
        }
    }

    @Schema(name = "ReportDelayedSlot", description = "Uma hora de um dia com prontos que esperaram mais de 5 min no passe")
    record Delayed(@Schema(requiredMode = REQUIRED) LocalDate day, @Schema(requiredMode = REQUIRED) int hour,
                   @Schema(requiredMode = REQUIRED) int items, @Schema(requiredMode = REQUIRED) long averageWaitSeconds) {
        static Delayed of(DelayedSlot d) {
            return new Delayed(d.day(), d.hour(), d.items(), d.averageWait().toSeconds());
        }
    }

    @Schema(name = "KitchenFull", description = "Os blocos do Business")
    record Full(@Schema(requiredMode = REQUIRED) Times current,
                @Schema(requiredMode = REQUIRED, description = "A comparação; se o período termina hoje, até a mesma hora")
                Times compared,
                @Schema(requiredMode = REQUIRED) List<Hour> byHour,
                @Schema(requiredMode = REQUIRED) List<Peak> peaks,
                @Schema(requiredMode = REQUIRED) List<Category> byCategory,
                @Schema(requiredMode = REQUIRED) List<Delayed> delayed) {
        static Full of(KitchenFull f) {
            return new Full(Times.of(f.current()), Times.of(f.compared()), f.byHour().stream().map(Hour::of).toList(),
                    f.peaks().stream().map(Peak::of).toList(), f.byCategory().stream().map(Category::of).toList(),
                    f.delayed().stream().map(Delayed::of).toList());
        }
    }
}
