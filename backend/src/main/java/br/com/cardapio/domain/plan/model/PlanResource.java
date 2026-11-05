package br.com.cardapio.domain.plan.model;

import br.com.cardapio.domain.shared.model.Limit;

/** O que o plano conta: mesas, pessoas da equipe e setores ativos. */
public enum PlanResource {
    TABLES("mesa ativa", "mesas ativas"),
    USERS("pessoa ativa na equipe", "pessoas ativas na equipe"),
    SECTORS("setor ativo", "setores ativos");

    private final String one;
    private final String many;

    PlanResource(String one, String many) {
        this.one = one;
        this.many = many;
    }

    /** "1 setor ativo", "20 mesas ativas". */
    public String count(int quantity) {
        return quantity + " " + (quantity == 1 ? one : many);
    }

    public Limit limitIn(Plan plan) {
        return switch (this) {
            case TABLES -> plan.maxTables();
            case USERS -> plan.maxUsers();
            case SECTORS -> plan.maxSectors();
        };
    }
}
