package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Duration;
import java.util.Objects;

/**
 * Os preparos de um produto num intervalo, somados: quantos e o tempo total (do resumo diário ou das
 * consultas diretas). A média sai daqui, depois de juntar as partes.
 */
public record PrepTotal(ProductId productId, String name, int preparations, long millis) {

    public PrepTotal {
        Objects.requireNonNull(productId);
        if (preparations < 0 || millis < 0) {
            throw new IllegalArgumentException("preparos e tempo não podem ser negativos");
        }
    }

    public PrepTotal plus(PrepTotal other) {
        return new PrepTotal(productId, name, preparations + other.preparations, millis + other.millis);
    }

    /** A média arredondada ao segundo, como a consulta direta faz. */
    public PrepAverage average() {
        return new PrepAverage(productId, name, preparations,
                Duration.ofSeconds(preparations == 0 ? 0 : Math.round(millis / 1000.0 / preparations)));
    }
}
