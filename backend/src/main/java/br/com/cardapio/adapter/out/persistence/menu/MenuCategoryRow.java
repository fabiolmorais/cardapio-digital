package br.com.cardapio.adapter.out.persistence.menu;

import java.util.UUID;

/** Projeção da lista de categorias: a categoria e quantos produtos ativos ela tem. */
public interface MenuCategoryRow {

    UUID getId();

    UUID getEstablishmentId();

    String getName();

    int getPosition();

    boolean getActive();

    long getActiveProducts();
}
