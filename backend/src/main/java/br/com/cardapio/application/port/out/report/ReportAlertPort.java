package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.AlertFinding;
import br.com.cardapio.domain.report.model.AlertSettings;
import br.com.cardapio.domain.report.model.AlertType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** A configuração e o estado dos alertas em tempo real (REL-78), e o que está atrasado agora. */
public interface ReportAlertPort {

    /** A configuração do restaurante; vazio = nunca mudou (vale o padrão). */
    Optional<AlertSettings> settings(EstablishmentId establishmentId);

    void saveSettings(EstablishmentId establishmentId, AlertSettings settings);

    /** Os restaurantes ativos com algum alerta ligado (sem configuração, vale o padrão: ligados). */
    List<EstablishmentId> candidates();

    /** Os itens dos pedidos enviados que chegaram à cozinha antes de {@code before} e não ficaram prontos. */
    Optional<AlertFinding> lateKitchen(EstablishmentId establishmentId, Instant before);

    /** Os chamados feitos antes de {@code before} que ninguém aceitou ainda. */
    Optional<AlertFinding> unansweredCalls(EstablishmentId establishmentId, Instant before);

    /** As comandas abertas antes de {@code before} que continuam abertas. */
    Optional<AlertFinding> longTabs(EstablishmentId establishmentId, Instant before);

    /** O último alerta do tipo; vazio = nunca. */
    Optional<Instant> lastSent(EstablishmentId establishmentId, AlertType type);

    /**
     * Marca o alerta do tipo como enviado em {@code now}, se o último não for depois de
     * {@code notAfter}. true: esta chamada marcou, e quem chamou envia; false: outra instância marcou.
     */
    boolean claim(EstablishmentId establishmentId, AlertType type, Instant now, Instant notAfter);
}
