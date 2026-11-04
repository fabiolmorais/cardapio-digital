package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.time.ZoneId;

public interface EstablishmentTimeZonePort {

    /** O fuso do restaurante (o relógio dos preços por dia e horário). */
    ZoneId timeZoneOf(EstablishmentId establishmentId);
}
