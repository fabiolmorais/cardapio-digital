package br.com.cardapio.domain.report.model;

/**
 * Como o restaurante quer o resumo diário (REL-76): por e-mail, no celular e a que horas.
 *
 * @param sendHour a hora do envio, no fuso do restaurante (0 a 23). É também onde o "dia" do resumo
 *                 começa e termina: às 2h, a sexta vai das 2h de sexta às 2h de sábado
 */
public record DailySummarySettings(boolean emailEnabled, boolean pushEnabled, int sendHour) {

    /** Sem configuração: os dois ligados, às 2h, depois do fechamento da maioria das casas. */
    public static final DailySummarySettings DEFAULT = new DailySummarySettings(true, true, 2);

    public DailySummarySettings {
        if (sendHour < 0 || sendHour > 23) {
            throw new IllegalArgumentException("a hora do resumo vai de 0 a 23");
        }
    }

    /** Algum dos dois está ligado? */
    public boolean active() {
        return emailEnabled || pushEnabled;
    }
}
