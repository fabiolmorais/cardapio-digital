package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.report.service.RatingAnalysis;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Duration;
import java.util.Optional;

/**
 * O que uma pessoa da equipe fez no período (REL-46 a 49, 54).
 *
 * @param calls        chamados que aceitou
 * @param response     a resposta média aos chamados que aceitou
 * @param delivered    itens que entregou
 * @param closedTabs   comandas que fechou
 * @param revenue      o que essas comandas faturaram
 * @param staffOrders  pedidos que lançou
 * @param tips         a taxa de serviço destinada a ela
 * @param ratings      as avaliações das mesas que ela mais atendeu (REL-55)
 * @param ratingStars  a soma das estrelas dessas avaliações
 */
public record WaiterStats(UserId userId, String name, int calls, Optional<Duration> response, int delivered,
                          int closedTabs, Money revenue, int staffOrders, Money tips, int ratings, long ratingStars) {

    /** A nota na tabela: null com menos de {@code RatingAnalysis.MIN_FOR_AVERAGE} avaliações. */
    public Double ratingAverage() {
        return RatingAnalysis.shownAverage(ratings, ratingStars);
    }
}
