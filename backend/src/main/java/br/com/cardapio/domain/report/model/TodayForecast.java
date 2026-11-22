package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/**
 * "Hoje, até agora" (REL-10): o que hoje já faturou, o que a casa costuma fechar num dia como hoje e
 * onde ela costuma estar nesta hora.
 *
 * @param expected a média do dia inteiro nos mesmos dias da semana anteriores
 * @param usual    a média, nesses dias, até esta mesma hora
 * @param weeks    quantos desses dias entraram na média (de 1 a 4; os sem venda ficam de fora)
 */
public record TodayForecast(Money soFar, Money expected, Money usual, int weeks) {
}
