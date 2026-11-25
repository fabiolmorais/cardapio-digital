package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.DailySummary;
import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.push.model.PushMessage;
import br.com.cardapio.domain.report.model.RatingStats;
import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.shared.model.Email;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Os textos do resumo diário (desenho 14.8), em pt-BR: o e-mail (texto simples e HTML) e o aviso no
 * celular. Tudo que vem do cadastro (o nome do restaurante, dos produtos) é escapado no HTML.
 */
final class DailySummaryMessages {

    static final String PUSH_TAG = "daily-summary";

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String[] WEEKDAYS = {"segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo"};
    private static final String[] MONTHS = {"janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho",
            "agosto", "setembro", "outubro", "novembro", "dezembro"};

    private static final String ACCENT = "#CC3F1C";
    private static final String UP = "#15803D";
    private static final String DOWN = "#B91C1C";
    private static final String MUTED = "#57534E";

    private DailySummaryMessages() {
    }

    /** Um número do resumo com a variação embaixo; {@code good}: a variação é boa (verde). */
    private record Tile(String label, String value, String delta, Boolean good) {
    }

    static EmailMessage email(DailySummary s, Email to, Role role, String publicUrl) {
        LocalDate day = s.window().day();
        String reportsUrl = reportsUrl(publicUrl, day);
        String settingsUrl = publicUrl + "/admin/relatorios/resumo";
        String title = "Seu dia: " + dayLabel(day);
        String subject = "Resumo de " + dayLabel(day) + " · " + money(s.current().revenue().cents());
        List<Tile> tiles = tiles(s);
        List<String[]> lines = lines(s);
        String footer = "Você recebe este resumo porque é " + (role == Role.OWNER ? "dono" : "gerente") + " do "
                + s.establishmentName() + " no OhChefe. Escolha o horário ou desligue em Gestão › Relatórios › "
                + "Resumo diário.";

        StringBuilder text = new StringBuilder();
        text.append("OhChefe · ").append(s.establishmentName()).append('\n').append(title).append("\n\n");
        for (Tile t : tiles) {
            text.append(t.label()).append(": ").append(t.value());
            if (t.delta() != null) {
                text.append(" (").append(t.delta()).append(')');
            }
            text.append('\n');
        }
        text.append('\n');
        s.goal().ifPresent(g -> text.append(goalTitle(g)).append(": ").append(goalText(g)).append("\n\n"));
        for (String[] line : lines) {
            text.append(line[0]).append(": ").append(line[1]).append('\n');
        }
        text.append("\nVer os relatórios: ").append(reportsUrl).append("\n\n").append(footer).append('\n')
                .append(settingsUrl).append('\n');

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><title>")
                .append(escape(title)).append("</title></head>")
                .append("<body style=\"margin:0;padding:24px 12px;background:#F5F2EE;")
                .append("font-family:Helvetica,Arial,sans-serif;color:#1C1917\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"max-width:600px;margin:0 auto;background:#FFFFFF;border-radius:16px;overflow:hidden\">")
                .append("<tr><td style=\"background:").append(ACCENT).append(";color:#FFFFFF;padding:20px 28px\">")
                .append("<div style=\"font-size:13px;font-weight:600;opacity:.9\">OhChefe · ")
                .append(escape(s.establishmentName())).append("</div>")
                .append("<div style=\"font-size:22px;font-weight:700;margin-top:2px\">").append(escape(title))
                .append("</div></td></tr><tr><td style=\"padding:24px 28px\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        for (int i = 0; i < tiles.size(); i += 2) {
            html.append("<tr>");
            for (int j = i; j < i + 2; j++) {
                html.append("<td width=\"50%\" style=\"padding:6px;vertical-align:top\">");
                if (j < tiles.size()) {
                    Tile t = tiles.get(j);
                    html.append("<div style=\"border:1px solid #E7E2DC;border-radius:12px;padding:14px 16px\">")
                            .append("<div style=\"font-size:13px;color:").append(MUTED).append("\">")
                            .append(escape(t.label())).append("</div>")
                            .append("<div style=\"font-size:24px;font-weight:700\">").append(escape(t.value()))
                            .append("</div>");
                    if (t.delta() != null) {
                        String color = t.good() == null ? MUTED : t.good() ? UP : DOWN;
                        html.append("<div style=\"font-size:13px;font-weight:600;color:").append(color).append("\">")
                                .append(escape(t.delta())).append("</div>");
                    }
                    html.append("</div>");
                }
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
        s.goal().ifPresent(g -> html.append(goalHtml(g)));
        html.append("<div style=\"font-size:14px;line-height:20px;margin:16px 6px\">");
        for (String[] line : lines) {
            html.append("<p style=\"margin:0 0 8px\"><b>").append(escape(line[0])).append(":</b> ")
                    .append(escape(line[1])).append("</p>");
        }
        html.append("</div><a href=\"").append(escape(reportsUrl)).append("\" style=\"display:inline-block;")
                .append("margin:0 6px;padding:12px 18px;border-radius:12px;background:").append(ACCENT)
                .append(";color:#FFFFFF;text-decoration:none;font-size:14px;font-weight:600\">Ver os relatórios</a>")
                .append("<p style=\"font-size:12px;color:#78716C;margin:20px 6px 0\">").append(escape(footer))
                .append(" <a href=\"").append(escape(settingsUrl)).append("\" style=\"color:").append(ACCENT)
                .append("\">Mudar o resumo</a></p></td></tr></table></body></html>");

        return new EmailMessage(to, subject, text.toString(), html.toString());
    }

    /** "Meta de novembro". */
    static String goalTitle(GoalProgress g) {
        return "Meta de " + MONTHS[g.month().getMonthValue() - 1];
    }

    /**
     * "R$ 124.300,00 de R$ 180.000,00 (69%). Faltam 11 dias: R$ 5.060,00 por dia batem a meta (a média
     * está em R$ 6.890,00)."; batida ou com o mês no fim, só o resultado.
     */
    static String goalText(GoalProgress g) {
        String sofar = money(g.achieved().cents()) + " de " + money(g.goal().cents()) + " ("
                + Math.round(g.share() * 100) + "%)";
        if (g.reached()) {
            return "batida! " + sofar + ".";
        }
        if (g.perDayNeeded().isEmpty()) {
            return sofar + ". O mês terminou sem bater a meta.";
        }
        String days = g.daysLeft() == 1 ? "Falta 1 dia: " : "Faltam " + g.daysLeft() + " dias: ";
        return sofar + ". " + days + money(g.perDayNeeded().get().cents()) + " por dia batem a meta"
                + g.dailyAverage().map(a -> " (a média está em " + money(a.cents()) + ")").orElse("") + ".";
    }

    private static String goalHtml(GoalProgress g) {
        long width = Math.min(100, Math.round(g.share() * 100));
        return "<div style=\"border:1px solid #E7E2DC;border-radius:12px;padding:14px 16px;margin:10px 6px 0\">"
                + "<div style=\"font-size:13px;color:" + MUTED + "\">" + escape(goalTitle(g)) + "</div>"
                + "<div style=\"font-size:14px;font-weight:600;margin:4px 0 8px\">" + escape(money(g.achieved().cents()))
                + " de " + escape(money(g.goal().cents())) + " · " + Math.round(g.share() * 100) + "%</div>"
                + "<div style=\"height:8px;border-radius:999px;background:#EFEAE4;overflow:hidden\">"
                + "<div style=\"height:8px;width:" + width + "%;background:" + ACCENT + ";border-radius:999px\"></div>"
                + "</div><div style=\"font-size:13px;color:" + MUTED + ";margin-top:8px\">"
                + escape(goalText(g)) + "</div></div>";
    }

    /** "Resumo de sexta" · "R$ 9.870,00 em 84 comandas, 9% acima da sexta passada." */
    static PushMessage push(DailySummary s) {
        LocalDate day = s.window().day();
        ReportTotals now = s.current();
        ReportTotals before = s.compared();
        StringBuilder body = new StringBuilder(money(now.revenue().cents())).append(" em ")
                .append(now.tabs() == 1 ? "1 comanda" : now.tabs() + " comandas");
        if (before.revenue().cents() > 0) {
            long change = Math.round((now.revenue().cents() - before.revenue().cents()) * 100.0
                    / before.revenue().cents());
            String last = lastWeek(day);
            body.append(change == 0 ? ", igual à " + last
                    : change > 0 ? ", " + change + "% acima " + of(last) : ", " + -change + "% abaixo " + of(last));
        }
        body.append(". Toque para ver.");
        return new PushMessage("Resumo de " + WEEKDAYS[day.getDayOfWeek().getValue() - 1], body.toString(),
                "/admin/relatorios?de=" + day + "&ate=" + day, PUSH_TAG);
    }

    private static List<Tile> tiles(DailySummary s) {
        ReportTotals now = s.current();
        ReportTotals before = s.compared();
        String last = lastWeek(s.window().day());
        List<Tile> tiles = new ArrayList<>();
        tiles.add(percentTile("Faturamento", money(now.revenue().cents()), now.revenue().cents(),
                before.revenue().cents(), " vs. " + last, last));
        Tile tabs;
        if (before.tabs() == 0) {
            tabs = new Tile("Comandas", String.valueOf(now.tabs()), "sem movimento " + in(last), null);
        } else {
            int diff = now.tabs() - before.tabs();
            tabs = new Tile("Comandas", String.valueOf(now.tabs()),
                    diff == 0 ? "— igual" : (diff > 0 ? "▲ " : "▼ ") + Math.abs(diff), diff == 0 ? null : diff > 0);
        }
        tiles.add(tabs);
        tiles.add(percentTile("Tíquete médio", money(now.averageTicket().cents()), now.averageTicket().cents(),
                before.averageTicket().cents(), "", last));
        s.averagePrep().ifPresent(prep -> tiles.add(prepTile(prep, s.comparedAveragePrep(), last)));
        return tiles;
    }

    private static Tile percentTile(String label, String value, long now, long before, String suffix, String last) {
        if (before == 0) {
            return new Tile(label, value, "sem movimento " + in(last), null);
        }
        double change = (now - before) * 100.0 / before;
        if (Math.abs(change) < 0.05) {
            return new Tile(label, value, "— igual" + suffix, null);
        }
        return new Tile(label, value, (change > 0 ? "▲ " : "▼ ") + decimal(Math.abs(change)) + "%" + suffix, change > 0);
    }

    private static Tile prepTile(Duration prep, Optional<Duration> before, String last) {
        String value = duration(prep);
        if (before.isEmpty()) {
            return new Tile("Preparo médio", value, "sem preparos " + in(last), null);
        }
        long diff = prep.toSeconds() - before.get().toSeconds();
        if (diff == 0) {
            return new Tile("Preparo médio", value, "— igual", null);
        }
        String change = duration(Duration.ofSeconds(Math.abs(diff)));
        return diff < 0 ? new Tile("Preparo médio", value, "▼ " + change + ", mais rápido", true)
                : new Tile("Preparo médio", value, "▲ " + change + ", mais lento", false);
    }

    private static List<String[]> lines(DailySummary s) {
        List<String[]> lines = new ArrayList<>();
        s.champion().ifPresent(item -> lines.add(new String[]{"Campeão do dia",
                item.name() + ", " + (item.quantity() == 1 ? "1 vendido" : item.quantity() + " vendidos") + "."}));
        s.peak().ifPresent(peak -> lines.add(new String[]{"Pico", peak.hour() + "h, com " + money(peak.revenue().cents())
                + " em " + (peak.tabs() == 1 ? "1 comanda" : peak.tabs() + " comandas") + " que chegaram nessa hora."}));
        s.attention().ifPresent(a -> lines.add(new String[]{"Atenção", a.name() + " levou " + duration(a.dayAverage())
                + " em média (normal: " + duration(a.usualAverage()) + ")."}));
        if (!s.payments().isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (PaymentTotal p : s.payments()) {
                String method = switch (p.method()) {
                    case PIX -> "Pix";
                    case CARD -> "Cartão";
                    case CASH -> "Dinheiro";
                };
                parts.add(method + " " + money(p.amount().cents()));
            }
            lines.add(new String[]{"Pagamentos", String.join(" · ", parts) + " (com a taxa de serviço)."});
        }
        s.rating().ifPresent(r -> lines.add(new String[]{"Avaliações", ratingText(r)}));
        return lines;
    }

    /** "Nota 4,6 de 5 em 12 avaliações (1 nota baixa)." */
    static String ratingText(RatingStats r) {
        String average = String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", r.average());
        String text = "Nota " + average + " de 5 em " + (r.count() == 1 ? "1 avaliação" : r.count() + " avaliações");
        if (r.low() > 0) {
            text += " (" + (r.low() == 1 ? "1 nota baixa" : r.low() + " notas baixas") + ")";
        }
        return text + ".";
    }

    static String reportsUrl(String publicUrl, LocalDate day) {
        return publicUrl + "/admin/relatorios?de=" + day + "&ate=" + day;
    }

    /** "sexta, 14 de novembro". */
    static String dayLabel(LocalDate day) {
        return WEEKDAYS[day.getDayOfWeek().getValue() - 1] + ", " + day.getDayOfMonth() + " de "
                + MONTHS[day.getMonthValue() - 1];
    }

    /** "sexta passada", "sábado passado". */
    private static String lastWeek(LocalDate day) {
        int weekday = day.getDayOfWeek().getValue();
        return WEEKDAYS[weekday - 1] + (weekday >= 6 ? " passado" : " passada");
    }

    /** "da sexta passada", "do sábado passado". */
    private static String of(String lastWeek) {
        return (lastWeek.endsWith("passado") ? "do " : "da ") + lastWeek;
    }

    /** "na sexta passada", "no sábado passado". */
    private static String in(String lastWeek) {
        return (lastWeek.endsWith("passado") ? "no " : "na ") + lastWeek;
    }

    /** "R$ 9.870,00". */
    static String money(long cents) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return "R$ " + format.format(BigDecimal.valueOf(cents, 2));
    }

    private static String decimal(double value) {
        DecimalFormat format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(PT_BR));
        return format.format(value);
    }

    /** Como a tela da cozinha: "45 s", "3 min 40 s", "12 min", "1 h 05 min". */
    static String duration(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        if (seconds < 60) {
            return seconds + " s";
        }
        if (seconds < 600) {
            long rest = seconds % 60;
            return rest == 0 ? seconds / 60 + " min" : seconds / 60 + " min " + rest + " s";
        }
        long minutes = Math.round(seconds / 60.0);
        if (minutes < 60) {
            return minutes + " min";
        }
        return minutes / 60 + " h " + String.format("%02d", minutes % 60) + " min";
    }

    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
