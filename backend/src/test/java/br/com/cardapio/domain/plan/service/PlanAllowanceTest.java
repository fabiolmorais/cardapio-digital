package br.com.cardapio.domain.plan.service;

import br.com.cardapio.domain.plan.exception.FeatureNotInPlanException;
import br.com.cardapio.domain.plan.exception.PlanLimitReachedException;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.PlanResource;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.SupportLevel;
import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanAllowanceTest {

    private static Plan plan(String name, Limit tables, Limit users, Limit sectors, Set<PlanFeature> features) {
        return new Plan(new PlanId(UUID.randomUUID()), name.toLowerCase(), name, Money.ofCents(6990),
                Money.ofCents(69900), tables, users, sectors, 90, features, SupportLevel.EMAIL, 1);
    }

    private final Plan essential = plan("Essencial", Limit.of(20), Limit.of(5), Limit.of(1), Set.of());
    private final Plan enterprise = plan("Enterprise", Limit.unlimited(), Limit.unlimited(), Limit.unlimited(),
            Set.of(PlanFeature.PRODUCT_VIDEOS));

    @Test
    void theLastSeatIsFreeAndTheNextOneIsNot() {
        assertThatCode(() -> PlanAllowance.checkRoomForOneMore(essential, new PlanUsage(19, 5, 1), PlanResource.TABLES))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PlanAllowance.checkRoomForOneMore(essential, new PlanUsage(20, 0, 0),
                PlanResource.TABLES))
                .isInstanceOf(PlanLimitReachedException.class)
                .hasMessage("O plano Essencial permite até 20 mesas ativas. Para ter mais, mude de plano.");
    }

    @Test
    void aPlanSmallerThanWhatIsInUseOnlyBlocksTheNextOne() {
        // Depois de mudar do Pro para o Essencial com 30 mesas: as 30 continuam, a 31ª não entra.
        assertThatThrownBy(() -> PlanAllowance.checkRoomForOneMore(essential, new PlanUsage(30, 0, 0),
                PlanResource.TABLES))
                .isInstanceOf(PlanLimitReachedException.class);
    }

    @Test
    void unlimitedNeverStopsAndVideosNeedTheFeature() {
        assertThatCode(() -> PlanAllowance.checkRoomForOneMore(enterprise, new PlanUsage(5000, 900, 80),
                PlanResource.USERS))
                .doesNotThrowAnyException();
        assertThatCode(() -> PlanAllowance.checkVideos(enterprise)).doesNotThrowAnyException();
        assertThatThrownBy(() -> PlanAllowance.checkVideos(essential)).isInstanceOf(FeatureNotInPlanException.class);
    }
}
