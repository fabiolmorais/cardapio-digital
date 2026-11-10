package br.com.cardapio.adapter.out.persistence.floor;

import java.util.UUID;

/** Projeção do movimento de uma mesa (consulta nativa em TableActivityPersistenceAdapter). */
interface TableActivityRow {

    UUID getTableId();

    boolean getTabOpen();

    boolean getBillRequested();

    boolean getRodizio();
}
