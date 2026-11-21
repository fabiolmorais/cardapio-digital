package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Duration;
import java.util.Optional;

/**
 * Um item entre os que mais demoram (REL-30): o preparo médio, o "9 em 10 até" e o pior horário.
 *
 * @param preparations  quantas vezes ficou pronto no período
 * @param ninetyPercent 9 em 10 preparos ficaram prontos até esse tempo (percentil 90)
 * @param worstHour     o dia da semana e a hora com o preparo médio mais alto, se alguma hora teve
 *                      preparos bastantes para comparar
 */
public record SlowItem(ProductId productId, String name, String category, int preparations, Duration average,
                       Duration ninetyPercent, Optional<WeekdayHour> worstHour) {
}
