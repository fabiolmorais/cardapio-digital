package br.com.cardapio.domain.plan.model;

/**
 * Recursos que variam entre os planos. Os recursos presentes em todos os planos
 * (chamar garçom, pedido pelo celular etc.) não aparecem aqui.
 */
public enum PlanFeature {
    PRODUCT_VIDEOS,
    PREP_STATIONS,
    ROLES_AND_PERMISSIONS,
    ADVANCED_REPORTS,
    REPORT_EXPORT,
    MULTI_UNIT
}
