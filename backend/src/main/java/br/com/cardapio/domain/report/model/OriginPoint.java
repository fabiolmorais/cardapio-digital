package br.com.cardapio.domain.report.model;

import java.time.LocalDate;
import java.util.Objects;

/** Um ponto de "quem lança os pedidos": um dia, uma semana ou um mês (cortado nas pontas do período). */
public record OriginPoint(LocalDate start, LocalDate end, int phone, int staff) {

    public OriginPoint {
        Objects.requireNonNull(start, "início é obrigatório");
        Objects.requireNonNull(end, "fim é obrigatório");
    }
}
