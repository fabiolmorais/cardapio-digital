package br.com.cardapio.adapter.out.persistence.floor;

import java.time.Instant;
import java.util.UUID;

/** Projeção de uma mesa no mapa ao vivo (consulta nativa em DiningTableJpaRepository.findLive). */
interface LiveTableRow {

    UUID getTableId();

    String getLabel();

    UUID getSectorId();

    String getSectorName();

    long getPhones();

    UUID getTabId();

    Instant getTabOpenedAt();

    long getTabTotalCents();

    long getItemsOnTheWay();

    long getRodizioPeople();
}
