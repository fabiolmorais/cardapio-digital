package br.com.cardapio.domain.plan.model;

/** Quanto o restaurante usa agora do que o plano conta (só os ativos). */
public record PlanUsage(long tables, long users, long sectors) {

    public long of(PlanResource resource) {
        return switch (resource) {
            case TABLES -> tables;
            case USERS -> users;
            case SECTORS -> sectors;
            // As unidades são contadas na empresa, e não em cada restaurante: ver PlanAllowance.
            case UNITS -> throw new IllegalArgumentException("unidades não fazem parte do uso do restaurante");
        };
    }
}
