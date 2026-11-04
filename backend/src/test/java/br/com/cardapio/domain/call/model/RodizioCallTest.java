package br.com.cardapio.domain.call.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RodizioCallTest {

    private static final Instant NOW = Instant.parse("2026-11-06T23:00:00Z");

    private final UserId carlos = UserId.newId();
    private final UserId ana = UserId.newId();

    private Call open(CallType type) {
        return Call.open(EstablishmentId.newId(), DiningTableId.newId(), TableSessionId.newId(), type, null, NOW);
    }

    @Test
    void activatingTheRodizioAnswersTheRequest() {
        Call pending = open(CallType.RODIZIO).resolveWithRodizio(carlos, NOW.plusSeconds(60));
        Call accepted = open(CallType.RODIZIO).accept(ana, NOW.plusSeconds(10))
                .resolveWithRodizio(carlos, NOW.plusSeconds(60));

        assertThat(pending.status()).isEqualTo(CallStatus.DONE);
        assertThat(pending.acceptedBy()).isEqualTo(carlos);
        assertThat(pending.acceptedAt()).isEqualTo(NOW.plusSeconds(60));
        // Quem aceitou antes continua sendo quem atendeu (é o tempo de resposta dele nos relatórios).
        assertThat(accepted.acceptedBy()).isEqualTo(ana);
        assertThat(accepted.acceptedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(accepted.doneAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void onlyAnOpenRodizioRequestEndsThisWay() {
        assertThatThrownBy(() -> open(CallType.WAITER).resolveWithRodizio(carlos, NOW))
                .isInstanceOf(CallStateException.class);
        Call canceled = open(CallType.RODIZIO).cancel(NOW);
        assertThatThrownBy(() -> canceled.resolveWithRodizio(carlos, NOW)).isInstanceOf(CallStateException.class);
    }
}
