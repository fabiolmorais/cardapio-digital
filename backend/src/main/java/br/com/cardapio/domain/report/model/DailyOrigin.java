package br.com.cardapio.domain.report.model;

import java.time.LocalDate;
import java.util.Objects;

/** Os pedidos enviados num dia: pelo celular do cliente e lançados pela equipe (REL-63). */
public record DailyOrigin(LocalDate day, int phone, int staff) {

    public DailyOrigin {
        Objects.requireNonNull(day, "dia é obrigatório");
    }
}
