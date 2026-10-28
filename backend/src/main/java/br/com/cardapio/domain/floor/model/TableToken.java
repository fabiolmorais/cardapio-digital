package br.com.cardapio.domain.floor.model;

import java.util.Objects;

/**
 * O token público da mesa: vai na URL do QR Code (/m/{token}). É aleatório e longo, então
 * não dá para adivinhar o da mesa ao lado trocando um número, como daria com /m/4, /m/5.
 *
 * Diferente do token do aparelho, este fica no banco como está: ele é impresso no display e
 * a página da mesa precisa achá-lo pelo valor. Quem o protege é ser imprevisível e poder
 * ser trocado (o QR antigo para de funcionar).
 */
public record TableToken(String value) {

    public TableToken {
        Objects.requireNonNull(value, "token da mesa é obrigatório");
        if (!value.matches("[A-Za-z0-9_-]{20,64}")) {
            throw new IllegalArgumentException("token da mesa fora do formato");
        }
    }

    @Override
    public String toString() {
        return "TableToken[protegido]";
    }
}
