package br.com.cardapio.domain.report.model;

import java.util.Objects;
import java.util.Optional;

/**
 * As mudanças de mesa do período (REL-62).
 *
 * @param moves        as comandas levadas para outra mesa
 * @param merges       as comandas juntadas a outra
 * @param topFrom      a mesa de onde o grupo mais saiu; vazio sem mudanças
 * @param topFromMoves quantas vezes saíram dela
 */
public record TableMoves(int moves, int merges, Optional<String> topFrom, int topFromMoves) {

    public TableMoves {
        Objects.requireNonNull(topFrom, "mesa de onde mais saem é obrigatória");
    }
}
