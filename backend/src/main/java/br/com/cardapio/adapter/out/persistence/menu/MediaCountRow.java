package br.com.cardapio.adapter.out.persistence.menu;

import java.util.UUID;

/** Projeção da contagem da galeria: o produto, o tipo da mídia e quantas são. */
interface MediaCountRow {

    UUID getProductId();

    String getType();

    long getTotal();
}
