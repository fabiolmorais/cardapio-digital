package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.report.model.DailySummarySettings;
import br.com.cardapio.domain.report.model.SummaryRecipient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A configuração e os destinatários do resumo diário (REL-76). */
public interface DailySummaryPort {

    /** A configuração do restaurante; vazio = nunca mudou (vale o padrão). */
    Optional<DailySummarySettings> settings(EstablishmentId establishmentId);

    void saveSettings(EstablishmentId establishmentId, DailySummarySettings settings);

    /** O dono e os gerentes ativos (com usuário ativo), do dono para os gerentes e por nome. */
    List<SummaryRecipient> recipients(EstablishmentId establishmentId);

    /** Recebem os desta lista; os outros donos e gerentes do restaurante deixam de receber. */
    void setReceivers(EstablishmentId establishmentId, Set<UserId> receivers);

    /** O restaurante, com a configuração (o padrão, se nunca mudou), o nome e o fuso. */
    Optional<SummaryTarget> target(EstablishmentId establishmentId);

    /** Os restaurantes ativos com o resumo ligado (sem configuração, vale o padrão: ligado). */
    List<SummaryTarget> targets();

    /**
     * Marca o dia como enviado, se ainda não estava (nem um dia depois dele). true: esta chamada
     * marcou, e quem chamou envia; false: outra instância já marcou.
     */
    boolean claim(EstablishmentId establishmentId, LocalDate day);
}
