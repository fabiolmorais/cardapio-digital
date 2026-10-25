package br.com.cardapio.domain.plan.model;

import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Plano de assinatura. Sem preço significa "sob consulta" (Enterprise).
 * {@code historyDays} igual a 0 significa histórico ilimitado.
 */
public record Plan(
        PlanId id,
        String code,
        String name,
        Money monthlyPrice,
        Money annualPrice,
        Limit maxTables,
        Limit maxUsers,
        Limit maxSectors,
        int historyDays,
        Set<PlanFeature> features,
        SupportLevel support,
        int displayOrder) {

    public Plan {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(maxTables, "limite de mesas é obrigatório");
        Objects.requireNonNull(maxUsers, "limite de usuários é obrigatório");
        Objects.requireNonNull(maxSectors, "limite de setores é obrigatório");
        Objects.requireNonNull(support, "suporte é obrigatório");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("código é obrigatório");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("nome é obrigatório");
        }
        if ((monthlyPrice == null) != (annualPrice == null)) {
            throw new IllegalArgumentException("plano com preço precisa de valor mensal e anual");
        }
        if (historyDays < 0) {
            throw new IllegalArgumentException("dias de histórico não pode ser negativo");
        }
        features = features == null || features.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(features));
    }

    /** Plano sem preço público: o valor é negociado ("sob consulta"). */
    public boolean isCustomPricing() {
        return monthlyPrice == null;
    }

    public Optional<Money> monthly() {
        return Optional.ofNullable(monthlyPrice);
    }

    public Optional<Money> annual() {
        return Optional.ofNullable(annualPrice);
    }

    public boolean hasUnlimitedHistory() {
        return historyDays == 0;
    }

    public boolean includes(PlanFeature feature) {
        return features.contains(feature);
    }

    public boolean allowsTables(int count) {
        return maxTables.allows(count);
    }
}
