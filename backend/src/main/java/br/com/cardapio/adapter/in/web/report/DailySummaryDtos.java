package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Item;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Payment;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Totals;
import br.com.cardapio.application.port.in.report.DailySummary;
import br.com.cardapio.application.port.in.report.DailySummarySettingsView;
import br.com.cardapio.application.port.in.report.SummaryTestResult;
import br.com.cardapio.domain.report.model.SummaryRecipient;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O resumo diário: a configuração, a prévia e o teste. */
final class DailySummaryDtos {

    private DailySummaryDtos() {
    }

    enum RecipientRole { OWNER, MANAGER }

    @Schema(name = "DailySummaryRecipient", description = "Um dono ou gerente que pode receber o resumo")
    record Recipient(@Schema(requiredMode = REQUIRED) UUID userId, @Schema(requiredMode = REQUIRED) String name,
                     @Schema(requiredMode = REQUIRED) RecipientRole role,
                     @Schema(requiredMode = REQUIRED, description = "Sem e-mail (entra só pelo PIN): só no celular")
                     boolean hasEmail,
                     @Schema(requiredMode = REQUIRED) boolean receives) {
        static Recipient of(SummaryRecipient r) {
            return new Recipient(r.userId().value(), r.name(), RecipientRole.valueOf(r.role().name()),
                    r.email().isPresent(), r.receives());
        }
    }

    @Schema(name = "DailySummarySettings")
    record Settings(@Schema(requiredMode = REQUIRED) boolean emailEnabled,
                    @Schema(requiredMode = REQUIRED) boolean pushEnabled,
                    @Schema(requiredMode = REQUIRED, description = "A hora do envio, no fuso do restaurante (0 a 23)")
                    int sendHour,
                    @Schema(requiredMode = REQUIRED) List<Recipient> recipients) {
        static Settings of(DailySummarySettingsView view) {
            return new Settings(view.settings().emailEnabled(), view.settings().pushEnabled(),
                    view.settings().sendHour(), view.recipients().stream().map(Recipient::of).toList());
        }
    }

    @Schema(name = "DailySummarySettingsRequest")
    record SettingsRequest(@Schema(requiredMode = REQUIRED) boolean emailEnabled,
                           @Schema(requiredMode = REQUIRED) boolean pushEnabled,
                           @Schema(requiredMode = REQUIRED, example = "2") @NotNull @Min(0) @Max(23) Integer sendHour,
                           @Schema(requiredMode = REQUIRED, description = "Quem recebe (donos e gerentes)")
                           @NotNull @Size(max = 50) List<@NotNull UUID> receiverIds) {
    }

    @Schema(name = "DailySummaryPeak", description = "A hora em que chegaram as mesas que mais faturaram")
    record Peak(@Schema(requiredMode = REQUIRED) int hour, @Schema(requiredMode = REQUIRED) long revenueCents,
                @Schema(requiredMode = REQUIRED) int tabs) {
    }

    @Schema(name = "DailySummaryAttention", description = "O item que demorou bem mais que o normal")
    record Attention(@Schema(requiredMode = REQUIRED) String name,
                     @Schema(requiredMode = REQUIRED) long dayAverageSeconds,
                     @Schema(requiredMode = REQUIRED, description = "A média dos 28 dias antes")
                     long usualAverageSeconds) {
    }

    @Schema(name = "DailySummary", description = "O resumo de um dia, como sai no e-mail")
    record Summary(@Schema(requiredMode = REQUIRED) String establishmentName,
                   @Schema(requiredMode = REQUIRED, description = "O dia em que a janela começa") LocalDate day,
                   @Schema(requiredMode = REQUIRED) Instant start, @Schema(requiredMode = REQUIRED) Instant end,
                   @Schema(requiredMode = REQUIRED) Totals current,
                   @Schema(requiredMode = REQUIRED, description = "O mesmo dia da semana anterior") Totals compared,
                   Long averagePrepSeconds, Long comparedAveragePrepSeconds, Item champion, Peak peak,
                   Attention attention, @Schema(requiredMode = REQUIRED) List<Payment> payments,
                   @Schema(description = "As avaliações do dia (Business); ausente sem nenhuma") DayRating rating) {
        static Summary of(DailySummary s) {
            return new Summary(s.establishmentName(), s.window().day(), s.window().start(), s.window().end(), Totals.of(s.current()),
                    Totals.of(s.compared()), s.averagePrep().map(Duration::toSeconds).orElse(null),
                    s.comparedAveragePrep().map(Duration::toSeconds).orElse(null),
                    s.champion().map(Item::of).orElse(null),
                    s.peak().map(p -> new Peak(p.hour(), p.revenue().cents(), p.tabs())).orElse(null),
                    s.attention().map(a -> new Attention(a.name(), a.dayAverage().toSeconds(),
                            a.usualAverage().toSeconds())).orElse(null),
                    s.payments().stream().map(Payment::of).toList(),
                    s.rating().map(r -> new DayRating(r.count(), r.average(), r.low())).orElse(null));
        }
    }

    @Schema(name = "DailySummaryRating", description = "As avaliações do atendimento no dia")
    record DayRating(@Schema(requiredMode = REQUIRED) int count, @Schema(requiredMode = REQUIRED) double average,
                     @Schema(requiredMode = REQUIRED, description = "Notas 1 e 2") int low) {
    }

    @Schema(name = "DailySummaryTestResult")
    record TestResult(@Schema(requiredMode = REQUIRED, description = "O e-mail saiu") boolean email,
                      @Schema(requiredMode = REQUIRED, description = "Em quantos aparelhos o aviso chegou")
                      int pushDevices) {
        static TestResult of(SummaryTestResult r) {
            return new TestResult(r.email(), r.pushDevices());
        }
    }
}
