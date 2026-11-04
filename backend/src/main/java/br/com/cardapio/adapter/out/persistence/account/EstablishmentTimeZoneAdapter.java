package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
class EstablishmentTimeZoneAdapter implements EstablishmentTimeZonePort {

    private final EstablishmentJpaRepository establishments;

    EstablishmentTimeZoneAdapter(EstablishmentJpaRepository establishments) {
        this.establishments = establishments;
    }

    @Override
    public ZoneId timeZoneOf(EstablishmentId establishmentId) {
        return ZoneId.of(establishments.findTimeZone(establishmentId.value()).orElseThrow());
    }
}
