package br.com.cardapio.adapter.out.persistence.floor;

import java.util.UUID;

/** Projeção da lista de setores: o setor e quantas mesas ativas ele tem. */
public interface SectorRow {

    UUID getId();

    UUID getEstablishmentId();

    String getName();

    int getPosition();

    boolean getActive();

    long getActiveTables();
}
