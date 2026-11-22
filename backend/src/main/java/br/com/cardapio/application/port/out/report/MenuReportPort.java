package br.com.cardapio.application.port.out.report;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.CategorySales;
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.ProductPair;
import br.com.cardapio.domain.report.model.ProductRow;
import br.com.cardapio.domain.report.model.VariantSales;
import br.com.cardapio.domain.report.model.VideoComparison;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.report.model.ProductSort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * As vendas por produto (ADR 0013): os itens das comandas fechadas no intervalo [início, fim), sem os
 * cancelados e sem as rodadas do rodízio, pela categoria de hoje do produto.
 */
public interface MenuReportPort {

    /** Por categoria, do maior faturamento para o menor. */
    List<CategorySales> byCategory(EstablishmentId establishmentId, Instant start, Instant end,
                                   Optional<SectorId> sector, Optional<MenuCategoryId> category);

    /**
     * Os produtos que venderam, numa página, na ordem pedida (o empate desempata pela outra medida e pelo
     * nome). Cada linha traz também a base da curva ABC (o que os produtos à frente somam e o total), o
     * faturamento no intervalo da comparação [comparedStart, comparedEnd) e as unidades canceladas.
     */
    ResultPage<ProductRow> products(EstablishmentId establishmentId, Instant start, Instant end,
                                    Instant comparedStart, Instant comparedEnd, Optional<SectorId> sector,
                                    Optional<MenuCategoryId> category, ProductSort sort, PageQuery page);

    /** Os itens cancelados nas comandas fechadas, por produto e motivo, somando as unidades. */
    List<CanceledProductCount> canceled(EstablishmentId establishmentId, Instant start, Instant end,
                                        Optional<SectorId> sector, Optional<MenuCategoryId> category);

    /** As unidades vendidas por tamanho (só os itens com tamanho). */
    List<VariantSales> sizes(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                             Optional<MenuCategoryId> category);

    /** As opções e os adicionais escolhidos (sem os sabores, que dividem o item), com o que somaram. */
    List<OptionSales> options(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                              Optional<MenuCategoryId> category);

    /** Os pares de produtos pedidos na mesma comanda, dos que mais aparecem juntos (até {@code limit}). */
    List<ProductPair> pairs(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                            int minTogether, int limit);

    /** Os produtos ativos do cardápio de sempre com vídeo pronto e sem, e o que cada grupo vendeu. */
    VideoComparison video(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                          Optional<MenuCategoryId> category);

    /**
     * Os produtos ativos do cardápio de sempre cadastrados até {@code createdUntil} que menos venderam no
     * período, do menos vendido (inclusive os que não venderam nada) para o mais.
     */
    List<ProductSales> slowSellers(EstablishmentId establishmentId, Instant start, Instant end,
                                   Optional<SectorId> sector, Optional<MenuCategoryId> category,
                                   Instant createdUntil, int limit);

    /** Quantos produtos ativos o cardápio de sempre tem (na categoria, se pedida). */
    int activeProducts(EstablishmentId establishmentId, Optional<MenuCategoryId> category);
}
