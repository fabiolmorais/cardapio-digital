package br.com.cardapio.adapter.out.persistence.account;

import java.util.UUID;

/**
 * Projeção do Spring Data: o resultado da consulta vem direto nestes getters,
 * sem carregar as entidades inteiras.
 */
public interface ActiveMembershipRow {

    UUID getMembershipId();

    UUID getEstablishmentId();

    String getEstablishmentName();

    String getEstablishmentSlug();

    String getRole();
}
