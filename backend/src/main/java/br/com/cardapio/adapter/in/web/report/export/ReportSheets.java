package br.com.cardapio.adapter.in.web.report.export;

import br.com.cardapio.application.port.in.report.KitchenReport;
import br.com.cardapio.application.port.in.report.MenuProduct;
import br.com.cardapio.application.port.in.report.MenuReport;
import br.com.cardapio.application.port.in.report.OverviewReport;
import br.com.cardapio.application.port.in.report.PhoneRodizioReport;
import br.com.cardapio.application.port.in.report.SalesReport;
import br.com.cardapio.application.port.in.report.TableReport;
import br.com.cardapio.application.port.in.report.TeamReport;
import br.com.cardapio.application.port.in.report.UnitComparison;
import br.com.cardapio.domain.report.model.RatingComment;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.TimeAverage;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static br.com.cardapio.adapter.in.web.report.export.Sheet.day;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.empty;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.decimal;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.minutes;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.money;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.number;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.percent;
import static br.com.cardapio.adapter.in.web.report.export.Sheet.text;

/**
 * As tabelas de cada tela dos relatórios (RF29), para o CSV, o Excel e o PDF: o que a tela mostra, em linhas, para levar à
 * planilha. Dinheiro em reais, tempos em minutos, datas no dia do restaurante.
 */
public final class ReportSheets {

    private static final Map<String, String> PAYMENT = Map.of("PIX", "Pix", "CARD", "Cartão", "CASH", "Dinheiro");
    private static final Map<String, String> CALL = Map.of("WAITER", "Chamar garçom", "BILL", "Pedir a conta",
            "UNLOCK", "Liberar presença", "RODIZIO", "Quero o rodízio");
    private static final Map<String, String> PRESENCE = Map.of("WIFI", "Wi-Fi do salão", "GPS", "GPS",
            "STAFF", "Liberado pelo garçom");
    private static final List<String> WEEKDAYS = List.of("segunda", "terça", "quarta", "quinta", "sexta", "sábado",
            "domingo");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReportSheets() {
    }

    private static Sheet.Cell average(ReportTotals totals) {
        return totals.tabs() == 0 ? empty() : money(totals.averageTicket());
    }

    private static double ratio(long part, long whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }

    /** Visão geral (14.1). */
    public static Sheet overview(OverviewReport r) {
        Sheet sheet = new Sheet("Visão geral", r.period()).section("Resumo", "Indicador", "Período", "Comparação")
                .row(text("Faturamento (R$)"), money(r.current().revenue()), money(r.compared().revenue()))
                .row(text("Comandas"), number(r.current().tabs()), number(r.compared().tabs()))
                .row(text("Tíquete médio (R$)"), average(r.current()), average(r.compared()));
        sheet.section("Faturamento por dia", "Dia", "Faturamento (R$)", "Dia comparado", "Faturamento comparado (R$)");
        r.byDay().forEach(d -> sheet.row(day(d.day()), money(d.revenue()), day(d.comparedDay()),
                money(d.comparedRevenue())));
        sheet.section("Forma de pagamento", "Forma", "Valor (R$)");
        r.payments().forEach(p -> sheet.row(text(PAYMENT.get(p.method().name())), money(p.amount())));
        sheet.section("Quando o salão vende", "Dia da semana", "Hora", "Faturamento médio (R$)");
        r.heatmap().forEach(h -> sheet.row(text(WEEKDAYS.get(h.isoWeekday() - 1)), number(h.hour()),
                money(h.average())));
        sheet.section("Itens mais vendidos", "Produto", "Quantidade", "Faturamento (R$)");
        r.topItems().forEach(i -> sheet.row(text(i.name()), number(i.quantity()), money(i.revenue())));
        if (r.full() != null) {
            sheet.section("Garçons", "Garçom", "Chamados", "Resposta média (min)");
            r.full().waiters().forEach(w -> sheet.row(text(w.name()), number(w.calls()),
                    minutes(w.averageResponse())));
        }
        return sheet;
    }

    /** Vendas (14.2). */
    public static Sheet sales(SalesReport r) {
        Sheet sheet = new Sheet("Vendas", r.period()).section("Resumo", "Indicador", "Período", "Comparação")
                .row(text("Faturamento (R$)"), money(r.current().revenue()), money(r.compared().revenue()))
                .row(text("Comandas"), number(r.current().tabs()), number(r.compared().tabs()))
                .row(text("Tíquete médio (R$)"), average(r.current()), average(r.compared()))
                .row(text("Comandas por dia"), decimal(r.tabsPerDay(), 1), decimal(r.comparedTabsPerDay(), 1));
        sheet.section("Faturamento no tempo", "Início", "Fim", "Faturamento (R$)", "Comandas");
        r.series().forEach(p -> sheet.row(day(p.start()), day(p.end()), money(p.revenue()), number(p.tabs())));
        sheet.section("Por hora", "Hora", "Faturamento por dia (R$)", "Comandas por dia");
        r.byHour().forEach(h -> sheet.row(number(h.hour()), money(h.revenuePerDay()), decimal(h.tabsPerDay(), 1)));
        var full = r.full();
        if (full != null) {
            sheet.section("Por setor", "Setor", "Mesas", "Faturamento (R$)", "Por mesa, por dia (R$)");
            full.bySector().forEach(s -> sheet.row(text(s.name()), number(s.tables()), money(s.revenue()),
                    money(s.perTablePerDay(full.days()))));
            sheet.section("Mesas que mais vendem", "Mesa", "Setor", "Comandas", "Faturamento (R$)", "Tíquete (R$)");
            full.topTables().forEach(t -> sheet.row(text(t.label()), text(t.sector()), number(t.tabs()),
                    money(t.revenue()), money(t.averageTicket())));
            sheet.section("Tíquete por pessoa", "Grupo", "Faturamento (R$)", "Pessoas", "Por pessoa (R$)")
                    .row(text("No rodízio"), money(full.rodizio().revenue()), number(full.rodizio().people()),
                            money(full.rodizio().average()))
                    .row(text("Fora do rodízio"), money(full.people().revenue()), number(full.people().people()),
                            money(full.people().average()));
            sheet.section("Cancelamentos", "Motivo", "Valor (R$)");
            full.reasons().forEach(c -> sheet.row(text(c.reason()), money(c.amount())));
        }
        return sheet;
    }

    /** Cardápio (14.3): a tabela inteira de produtos, por faturamento. */
    public static Sheet menu(MenuReport r, List<MenuProduct> products) {
        Sheet sheet = new Sheet("Cardápio", r.period()).section("Resumo", "Indicador", "Valor")
                .row(text("Faturamento dos itens (R$)"), money(r.itemsRevenue()))
                .row(text("Itens vendidos"), number(r.itemsSold()))
                .row(text("Produtos ativos"), number(r.activeProducts()));
        boolean full = r.full() != null;
        if (full) {
            sheet.section("Produtos", "Produto", "Categoria", "Quantidade", "Faturamento (R$)", "Curva ABC",
                    "Faturamento anterior (R$)", "Cancelados");
        } else {
            sheet.section("Produtos", "Produto", "Categoria", "Quantidade", "Faturamento (R$)");
        }
        for (MenuProduct p : products) {
            var s = p.sales();
            if (full) {
                sheet.row(text(s.name()), text(s.category()), number(s.quantity()), money(s.revenue()),
                        text(p.abc() == null ? "" : p.abc().name()), money(p.compared()),
                        p.canceled() == null ? empty() : number(p.canceled()));
            } else {
                sheet.row(text(s.name()), text(s.category()), number(s.quantity()), money(s.revenue()));
            }
        }
        sheet.section("Por categoria", "Categoria", "Quantidade", "Faturamento (R$)");
        r.byCategory().forEach(c -> sheet.row(text(c.name()), number(c.quantity()), money(c.revenue())));
        sheet.section("Vendem pouco", "Produto", "Categoria", "Quantidade", "Faturamento (R$)");
        r.slowSellers().forEach(s -> sheet.row(text(s.name()), text(s.category()), number(s.quantity()),
                money(s.revenue())));
        if (full) {
            var interest = r.full().interest();
            sheet.section("Visto x pedido", "Produto", "Celulares que viram", "Pediram", "Conversão (%)");
            interest.viewed().forEach(i -> sheet.row(text(i.name()), number(i.viewers()), number(i.ordered()),
                    percent(i.conversion())));
            sheet.section("Reels mais vistos", "Produto", "Celulares que viram", "Pediram", "Conversão (%)");
            interest.reels().forEach(i -> sheet.row(text(i.name()), number(i.viewers()), number(i.ordered()),
                    percent(i.conversion())));
            sheet.section("Carrinhos", "Indicador", "Valor")
                    .row(text("Celulares que puseram algo no carrinho"), number(interest.carts().withCart()))
                    .row(text("Carrinhos não enviados"), number(interest.carts().notSent()))
                    .row(text("Ficou no carrinho (R$, preço de hoje)"), money(interest.leftValue()));
            sheet.section("Ficam no carrinho", "Produto", "Celulares", "Unidades");
            interest.mostLeft().forEach(l -> sheet.row(text(l.name()), number(l.sessions()), number(l.units())));
        }
        return sheet;
    }

    /** Cozinha e tempos (14.4). */
    public static Sheet kitchen(KitchenReport r) {
        Sheet sheet = new Sheet("Cozinha e tempos", r.period()).section("Resumo", "Indicador", "Período", "Comparação")
                .row(text("Preparo médio (min)"), minutes(r.averagePrep()), minutes(r.comparedAveragePrep()))
                .row(text("Itens preparados"), number(r.preparations()), empty());
        sheet.section("Itens que mais demoram", "Produto", "Categoria", "Preparos", "Média (min)", "9 em 10 até (min)",
                "Pior dia", "Pior hora");
        r.slowest().forEach(s -> sheet.row(text(s.name()), text(s.category()), number(s.preparations()),
                minutes(s.average()), minutes(s.ninetyPercent()),
                text(s.worstHour().map(w -> WEEKDAYS.get(w.isoWeekday() - 1)).orElse("")),
                s.worstHour().map(w -> number(w.hour())).orElse(empty())));
        var full = r.full();
        if (full != null) {
            sheet.section("Tempos do serviço", "Indicador", "Período", "Comparação")
                    .row(text("Esperando no passe (min)"), minutes(full.current().passe()),
                            minutes(full.compared().passe()))
                    .row(text("Do pedido à mesa (min)"), minutes(full.current().toTable()),
                            minutes(full.compared().toTable()))
                    .row(text("Esperando confirmação"), number(full.current().lateConfirmations()),
                            number(full.compared().lateConfirmations()));
            sheet.section("Por hora", "Hora", "Preparo (min)", "Passe (min)", "Itens");
            full.byHour().forEach(h -> sheet.row(number(h.hour()), minutes(h.prep()), minutes(h.passe()),
                    number(h.items())));
            sheet.section("Preparo por categoria", "Categoria", "Preparos", "Média (min)");
            full.byCategory().forEach(c -> sheet.row(text(c.category()), number(c.preparations()),
                    minutes(c.average())));
        }
        return sheet;
    }

    /** Atendimento e equipe (14.5). */
    private static final Map<String, String> RATING_TAGS = Map.of("SPEED", "Rapidez", "FRIENDLINESS", "Simpatia",
            "FOOD", "Comida", "AMBIENCE", "Ambiente");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    /** Atendimento e equipe (14.5), com as avaliações (14.16) e os comentários do período. */
    public static Sheet team(TeamReport r, List<RatingComment> comments) {
        Sheet sheet = new Sheet("Atendimento e equipe", r.period()).section("Resumo", "Indicador", "Período", "Comparação")
                .row(text("Chamados"), number(r.current().calls()), number(r.compared().calls()))
                .row(text("Resposta ao chamado (min)"), minutes(r.current().response()),
                        minutes(r.compared().response()))
                .row(text("Até concluir (min)"), minutes(r.current().done()), minutes(r.compared().done()))
                .row(text("Da conta pedida ao fechamento (min)"), minutes(r.current().billToClose()),
                        minutes(r.compared().billToClose()));
        sheet.section("Garçons", "Garçom", "Chamados", "Resposta (min)", "Itens entregues", "Comandas fechadas",
                "Faturamento (R$)", "Pedidos lançados", "Gorjeta (R$)", "Avaliações", "Nota");
        r.waiters().forEach(w -> sheet.row(text(w.name()), number(w.calls()), minutes(w.response()),
                number(w.delivered()), number(w.closedTabs()), money(w.revenue()), number(w.staffOrders()),
                money(w.tips()), number(w.ratings()), decimal(w.ratingAverage(), 1)));
        sheet.section("Resposta por hora", "Hora", "Resposta (min)", "Chamados");
        r.responseByHour().forEach(h -> sheet.row(number(h.hour()), minutes(h.average()), number(h.count())));
        sheet.section("Chamados por tipo", "Tipo", "Chamados", "Cancelados pelo cliente");
        r.byType().forEach(c -> sheet.row(text(CALL.get(c.type().name())), number(c.calls()), number(c.canceled())));
        sheet.section("Mesas que mais chamam", "Mesa", "Setor", "Chamados", "Comandas");
        r.topTables().forEach(t -> sheet.row(text(t.label()), text(t.sector()), number(t.calls()), number(t.tabs())));
        sheet.section("Trava de pedido repetido", "Indicador", "Valor")
                .row(text("Avisos mostrados"), number(r.warnings().shown()))
                .row(text("Garçom confirmou"), number(r.warnings().confirmed()))
                .row(text("Garçom tirou o item"), number(r.warnings().shown() - r.warnings().confirmed()));
        var ratings = r.ratings();
        sheet.section("Avaliações", "Indicador", "Valor")
                .row(text("Nota média"), decimal(ratings.average(), 1))
                .row(text("Nota média na comparação"), decimal(r.comparedRatingAverage(), 1))
                .row(text("Avaliações"), number(ratings.count()))
                .row(text("Comandas fechadas"), number(ratings.closedTabs()))
                .row(text("Notas baixas (1 ou 2)"), number(ratings.low()))
                .row(text("Notas 4 ou 5"), number(ratings.high()))
                .row(text("Foram ao Google"), number(ratings.googleClicks()))
                .row(text("Nota no almoço (até 16h)"), decimal(ratings.lunch().average(), 1))
                .row(text("Nota no jantar (das 16h)"), decimal(ratings.dinner().average(), 1));
        sheet.section("Nota ao longo do período", "De", "Até", "Avaliações", "Nota");
        r.ratingTrend().forEach(p -> sheet.row(day(p.from()), day(p.to()), number(p.count()),
                decimal(p.average(), 1)));
        sheet.section("Notas", "Estrelas", "Avaliações");
        for (int stars = 5; stars >= 1; stars--) {
            sheet.row(number(stars), number(ratings.byStars().get(stars - 1)));
        }
        sheet.section("Atalhos", "Atalho", "Nos elogios (4 ou 5)", "No que melhorar (1 a 3)");
        RATING_TAGS.keySet().stream().sorted().forEach(tag -> sheet.row(text(RATING_TAGS.get(tag)),
                number(ratings.praisedTags().getOrDefault(tag, 0)),
                number(ratings.improveTags().getOrDefault(tag, 0))));
        sheet.section("Comentários", "Dia", "Hora", "Mesa", "Garçom", "Nota", "Atalhos", "Comentário");
        comments.forEach(c -> {
            var at = c.createdAt().atZone(r.period().zone());
            sheet.row(day(at.toLocalDate()), text(HOUR.format(at)), text(c.tableLabel()),
                    text(c.waiterName() == null ? "" : c.waiterName()), number(c.stars()),
                    text(String.join(", ", c.tags().stream().map(RATING_TAGS::get).toList())), text(c.comment()));
        });
        return sheet;
    }

    /** Mesas e ocupação (14.6). */
    public static Sheet tables(TableReport r, ZoneId zone) {
        Sheet sheet = new Sheet("Mesas e ocupação", r.period()).section("Resumo", "Indicador", "Período", "Comparação")
                .row(text("Comandas fechadas"), number(r.current().tabs()), number(r.compared().tabs()))
                .row(text("Permanência média (min)"), minutes(r.current().stay()), minutes(r.compared().stay()))
                .row(text("Permanência no rodízio (min)"), minutes(r.current().rodizioStay()),
                        minutes(r.compared().rodizioStay()))
                .row(text("Permanência à la carte (min)"), minutes(r.current().aLaCarteStay()),
                        minutes(r.compared().aLaCarteStay()))
                .row(text("Giro (comandas por mesa por dia)"), decimal(r.turnover().orElse(null), 2),
                        decimal(r.comparedTurnover().orElse(null), 2))
                .row(text("Por hora de mesa ocupada (R$)"), money(r.perOccupiedHour()),
                        money(r.comparedPerOccupiedHour()));
        sheet.section("Mesas", "Mesa", "Setor", "Comandas", "Faturamento (R$)", "Tíquete (R$)");
        r.tables().forEach(t -> sheet.row(text(t.label()), text(t.sector()), number(t.tabs()), money(t.revenue()),
                money(t.ticket())));
        sheet.section("Ocupação por hora", "Hora", "Mesas ocupadas (%)");
        r.occupancy().byHour().forEach(h -> sheet.row(number(h.hour()), percent(h.share())));
        sheet.section("Comandas esquecidas abertas", "Mesa", "Aberta em", "Fechada em");
        r.forgotten().latest().forEach(f -> sheet.row(text(f.label()), text(WHEN.format(f.openedAt().atZone(zone))),
                text(f.closedAt().map(c -> WHEN.format(c.atZone(zone))).orElse("ainda aberta"))));
        sheet.section("Mudanças de mesa", "Indicador", "Valor")
                .row(text("Mudanças"), number(r.moves().moves()))
                .row(text("Junções de comandas"), number(r.moves().merges()))
                .row(text("Mesa de onde mais saem"), text(r.moves().topFrom().orElse("")));
        return sheet;
    }

    /** Celular e rodízio (14.7). */
    public static Sheet phoneAndRodizio(PhoneRodizioReport r) {
        Sheet sheet = new Sheet("Celular e rodízio", r.period()).section("Quem lança os pedidos", "Início", "Fim", "Pelo celular", "Pelo garçom",
                "Celular (%)");
        r.origin().forEach(p -> sheet.row(day(p.start()), day(p.end()), number(p.phone()), number(p.staff()),
                percent(ratio(p.phone(), p.phone() + p.staff()))));
        var f = r.funnel();
        var p = r.phones();
        sheet.section("Do QR ao pedido", "Indicador", "Valor")
                .row(text("Comandas no período"), number(f.tabs()))
                .row(text("Alguém escaneou o QR"), number(f.scanned()))
                .row(text("Pediram pelo celular"), number(f.phoneOrdered()))
                .row(text("Celulares por mesa"), decimal(p.phonesPerTab().orElse(null), 1))
                .row(text("Celulares que pedem por mesa"), decimal(p.orderingPerTab().orElse(null), 1))
                .row(text("Do QR ao primeiro pedido (min)"), minutes(p.scanToOrder()))
                .row(text("Mesas que escanearam e não pediram"), number(p.scannedNoOrder()))
                .row(text("Pedidos recusados pelo garçom"), number(p.rejected()))
                .row(text("Celulares bloqueados"), number(p.blocked()));
        sheet.section("Como a presença foi confirmada", "Forma", "Celulares");
        r.presence().forEach(c -> sheet.row(text(PRESENCE.get(c.method().name())), number(c.sessions())));
        var z = r.rodizio();
        sheet.section("Rodízio", "Indicador", "Valor")
                .row(text("Comandas com rodízio"), number(z.tabs()))
                .row(text("Pessoas"), number(z.people()))
                .row(text("Faturamento do rodízio (R$)"), money(z.revenue()))
                .row(text("À parte (R$)"), money(z.extra()))
                .row(text("Rodadas"), number(z.rounds()))
                .row(text("Itens pedidos"), number(z.items()))
                .row(text("Tempo médio (min)"), minutes(z.time()))
                .row(text("Mesas com tempo limite"), number(z.withLimit()))
                .row(text("Chegaram ao tempo limite"), number(z.reachedLimit()));
        sheet.section("Pessoas por faixa", "Faixa", "Pessoas");
        r.tiers().forEach(t -> sheet.row(text(t.tier()), number(t.people())));
        sheet.section("Mais pedidos no rodízio", "Produto", "Quantidade");
        r.topItems().forEach(i -> sheet.row(text(i.name()), number(i.quantity())));
        Duration serving = r.cardTime().serving();
        Duration paused = r.cardTime().paused();
        sheet.section("Passagem: cartão", "Cor", "Tempo (min)")
                .row(text("Verde"), minutes(serving))
                .row(text("Vermelho"), minutes(paused));
        sheet.section("Cortes mais pedidos", "Corte", "Pedidos");
        r.topCuts().forEach(c -> sheet.row(text(c.name()), number(c.quantity())));
        return sheet;
    }

    /**
     * A comparação entre unidades (REL-80): a tabela lado a lado (com a rede no fim) e a evolução de
     * cada unidade, ponto a ponto.
     */
    public static Sheet units(UnitComparison c) {
        Sheet sheet = new Sheet("Unidades", c.period()).section("Unidades", "Unidade", "Faturamento (R$)",
                "Faturamento anterior (R$)", "Comandas", "Comandas anteriores", "Tíquete médio (R$)",
                "Preparo médio (min)", "Resposta ao chamado (min)", "Nota média", "Avaliações");
        c.units().forEach(u -> sheet.row(text(u.name()), money(u.current().revenue()), money(u.compared().revenue()),
                number(u.current().tabs()), number(u.compared().tabs()), average(u.current()),
                time(u.prep()), time(u.callResponse()), decimal(u.ratingAverage(), 1), number(u.ratings().count())));
        var n = c.network();
        sheet.row(text("Rede"), money(n.current().revenue()), money(n.compared().revenue()),
                number(n.current().tabs()), number(n.compared().tabs()), average(n.current()),
                time(n.prep()), time(n.callResponse()), decimal(n.ratingAverage(), 1), number(n.ratings().count()));
        sheet.section("Evolução", "Unidade", "Início", "Fim", "Faturamento por dia (R$)", "Comandas por dia");
        c.units().forEach(u -> u.series().forEach(p -> sheet.row(text(u.name()), day(p.start()), day(p.end()),
                money(p.revenuePerDay()), decimal(p.tabsPerDay(), 1))));
        return sheet;
    }

    private static Sheet.Cell time(TimeAverage average) {
        return minutes(average.average());
    }
}
