package br.com.cardapio.domain.account.model;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Uma unidade ativa da empresa, vista por uma pessoa: o nome, o fuso e se ela é dona dessa unidade
 * (vínculo ativo de dono).
 */
public record UnitMembership(EstablishmentId id, String name, ZoneId zone, boolean owner) {

    public UnitMembership {
        Objects.requireNonNull(id, "unidade é obrigatória");
        Objects.requireNonNull(name, "nome é obrigatório");
        Objects.requireNonNull(zone, "fuso é obrigatório");
    }
}
