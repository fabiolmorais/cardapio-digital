package br.com.cardapio.adapter.out.persistence.tablesession;

import java.time.Instant;
import java.util.UUID;

/** Projeção da lista de celulares bloqueados: a sessão, a mesa, o setor e quem bloqueou. */
interface BlockedPhoneRow {

    UUID getId();

    UUID getTableId();

    String getTableLabel();

    String getSectorName();

    Instant getBlockedAt();

    String getBlockedByName();
}
