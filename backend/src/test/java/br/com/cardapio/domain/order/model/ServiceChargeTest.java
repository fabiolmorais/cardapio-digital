package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A taxa de serviço opcional, os 10% do garçom (RF39). */
class ServiceChargeTest {

    private static final Instant NOW = Instant.parse("2026-11-20T23:00:00Z");
    private static final ServiceChargeSettings ON = new ServiceChargeSettings(true, 10, ServiceDestination.TEAM);

    private final Tab tab = Tab.open(EstablishmentId.newId(), DiningTableId.newId(), NOW.minusSeconds(3600));

    @Test
    void itRoundsToTheNearestCent() {
        assertThat(new ServiceCharge(10, true).amountOn(Money.ofCents(379_30))).isEqualTo(Money.ofCents(37_93));
        assertThat(new ServiceCharge(12, true).amountOn(Money.ofCents(19_90))).isEqualTo(Money.ofCents(2_39));
        assertThat(new ServiceCharge(10, true).amountOn(Money.ofCents(4_95))).isEqualTo(Money.ofCents(50));
        assertThat(new ServiceCharge(10, false).chargedOn(Money.ofCents(100_00))).isEqualTo(Money.ZERO);
    }

    @Test
    void thePercentGoesFromOneToTwenty() {
        assertThatThrownBy(() -> new ServiceCharge(0, true)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ServiceChargeSettings(true, 21, ServiceDestination.TEAM))
                .isInstanceOf(InvalidValueException.class);
        assertThat(ServiceChargeSettings.DEFAULT.enabled()).isFalse();
        assertThat(ServiceChargeSettings.DEFAULT.percent()).isEqualTo(10);
    }

    @Test
    void itComesAcceptedUntilSomeoneRemovesItAndThePercentStaysTheCopiedOne() {
        assertThat(tab.effectiveService(ServiceChargeSettings.DEFAULT)).isNull();
        assertThat(tab.effectiveService(ON)).isEqualTo(new ServiceCharge(10, true));

        Tab removed = tab.chooseService(ON, false);
        Tab later = removed.chooseService(new ServiceChargeSettings(true, 15, ServiceDestination.TEAM), true);

        assertThat(removed.service()).isEqualTo(new ServiceCharge(10, false));
        assertThat(later.service()).isEqualTo(new ServiceCharge(10, true));
        // Restaurante sem taxa e ninguém escolheu antes: nada muda.
        assertThat(tab.chooseService(ServiceChargeSettings.DEFAULT, true)).isSameAs(tab);
    }

    @Test
    void theBalanceAddsTheServiceButTheTotalStaysTheConsumption() {
        TabBalance with = TabBalance.of(List.of(), Money.ofCents(100_00), new ServiceCharge(10, true),
                List.of(new TabPayment(TabPaymentId.newId(), tab.establishmentId(), tab.id(), Money.ofCents(50_00),
                        PaymentMethod.PIX, NOW, UserId.newId(), null)));
        TabBalance without = TabBalance.of(List.of(), Money.ofCents(100_00), new ServiceCharge(10, false), List.of());

        assertThat(with.total()).isEqualTo(Money.ofCents(100_00));
        assertThat(with.service()).isEqualTo(Money.ofCents(10_00));
        assertThat(with.due()).isEqualTo(Money.ofCents(60_00));
        assertThat(without.due()).isEqualTo(Money.ofCents(100_00));
    }

    @Test
    void closingChargesTheServiceApartFromTheTotal() {
        Tab chosen = tab.withService(new ServiceCharge(10, true));

        Tab.Closing closing = chosen.close(UserId.newId(), PaymentMethod.CARD, List.of(), Money.ofCents(129_90),
                List.of(), NOW);

        assertThat(closing.tab().total()).isEqualTo(Money.ofCents(129_90));
        assertThat(closing.tab().serviceCharged()).isEqualTo(Money.ofCents(12_99));
        assertThat(closing.finalPayment().amount()).isEqualTo(Money.ofCents(142_89));
    }

    private static final ServiceChargeSettings CUSTOM = new ServiceChargeSettings(true, 10, ServiceDestination.TEAM,
            true);

    @Test
    void anotherAmountReplacesThePercentOnlyWhenAccepted() {
        ServiceCharge five = new ServiceCharge(10, true, Money.ofCents(5_00));

        assertThat(five.amountOn(Money.ofCents(379_30))).isEqualTo(Money.ofCents(5_00));
        assertThat(five.percentOn(Money.ofCents(379_30))).isEqualTo(Money.ofCents(37_93));
        assertThat(five.chargedOn(Money.ofCents(379_30))).isEqualTo(Money.ofCents(5_00));
        // Tirar ou voltar ao percentual esquece o outro valor.
        assertThat(five.accept(false)).isEqualTo(new ServiceCharge(10, false));
        assertThat(five.accept(true).custom()).isNull();
        assertThatThrownBy(() -> new ServiceCharge(10, false, Money.ofCents(5_00)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ServiceCharge(10, true, Money.ZERO)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ServiceCharge(10, true, Money.ofCents(100_001)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ServiceChoice(false, Money.ofCents(5_00)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void anotherAmountOnlyWhenTheRestaurantAllows() {
        assertThat(ServiceChargeSettings.DEFAULT.customAllowed()).isFalse();
        assertThatThrownBy(() -> tab.chooseService(ON, ServiceChoice.custom(Money.ofCents(10_00))))
                .isInstanceOf(InvalidValueException.class);

        Tab ten = tab.chooseService(CUSTOM, ServiceChoice.custom(Money.ofCents(10_00)));
        assertThat(ten.service()).isEqualTo(new ServiceCharge(10, true, Money.ofCents(10_00)));
        // O restaurante desligou depois: a escolha já feita continua valendo, mas não troca por outra.
        assertThat(ten.chooseService(ON, ServiceChoice.custom(Money.ofCents(10_00)))).isEqualTo(ten);
        assertThatThrownBy(() -> ten.chooseService(ON, ServiceChoice.custom(Money.ofCents(20_00))))
                .isInstanceOf(InvalidValueException.class);
        assertThat(ten.chooseService(ON, ServiceChoice.PERCENT).service()).isEqualTo(new ServiceCharge(10, true));
        // Sem taxa no restaurante, nada muda, nem com outro valor.
        assertThat(tab.chooseService(ServiceChargeSettings.DEFAULT, ServiceChoice.custom(Money.ofCents(5_00))))
                .isSameAs(tab);
    }

    @Test
    void closingChargesTheOtherAmountApartFromTheTotal() {
        Tab chosen = tab.chooseService(CUSTOM, ServiceChoice.custom(Money.ofCents(5_00)));

        Tab.Closing closing = chosen.close(UserId.newId(), PaymentMethod.PIX, List.of(), Money.ofCents(129_90),
                List.of(), NOW);

        assertThat(closing.tab().total()).isEqualTo(Money.ofCents(129_90));
        assertThat(closing.tab().serviceCharged()).isEqualTo(Money.ofCents(5_00));
        assertThat(closing.finalPayment().amount()).isEqualTo(Money.ofCents(134_90));
    }
}
