package br.com.cardapio.domain.plan.model;

/**
 * Recursos que variam entre os planos. Os recursos presentes em todos os planos
 * (chamar garçom, pedido pelo celular etc.) não aparecem aqui.
 */
public enum PlanFeature {
    PRODUCT_VIDEOS,
    PREP_STATIONS,
    ROLES_AND_PERMISSIONS,
    /** Relatórios básicos (vendas, cardápio, picos e tempos de preparo): Pro em diante. */
    BASIC_REPORTS,
    /** Relatórios completos (equipe, atendimento, ocupação, metas e alertas): Business em diante. */
    REPORTS,
    REPORT_EXPORT,
    MULTI_UNIT
}
