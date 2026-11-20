package br.com.cardapio.adapter.out.persistence.order;

import java.time.Instant;
import java.util.UUID;

/** O cabeçalho da via "MUDOU DE MESA": as mesas e o setor da nova (os itens vêm numa segunda consulta). */
public record MovePrintRow(UUID moveId, UUID mergedTabId, String fromTableLabel, String toTableLabel,
                           String sectorName, Instant movedAt, boolean needsPrint, Instant printedAt) {
}
