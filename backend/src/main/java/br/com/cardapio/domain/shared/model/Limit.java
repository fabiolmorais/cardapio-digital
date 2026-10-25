package br.com.cardapio.domain.shared.model;

/**
 * Limite numérico de um plano. Sem valor significa ilimitado.
 */
public record Limit(Integer max) {

    private static final Limit UNLIMITED = new Limit(null);

    public Limit {
        if (max != null && max < 0) {
            throw new IllegalArgumentException("Limite não pode ser negativo");
        }
    }

    public static Limit of(int max) {
        return new Limit(max);
    }

    public static Limit unlimited() {
        return UNLIMITED;
    }

    public boolean isUnlimited() {
        return max == null;
    }

    /** Indica se é possível ter {@code count} itens dentro do limite. */
    public boolean allows(int count) {
        return isUnlimited() || count <= max;
    }
}
