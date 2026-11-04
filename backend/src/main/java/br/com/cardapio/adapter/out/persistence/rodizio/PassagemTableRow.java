package br.com.cardapio.adapter.out.persistence.rodizio;

import java.time.Instant;
import java.util.UUID;

/** Uma linha da tela dos passadores, montada pelo JPQL (o "new" da consulta). */
public record PassagemTableRow(UUID tableId, String tableLabel, String sectorName, String rodizioName, Long people,
                               String status, Instant statusChangedAt) {
}
