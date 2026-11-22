package br.com.cardapio.domain.report.model;

import java.time.Duration;
import java.time.LocalDate;

/** Uma hora de um dia com itens prontos que demoraram a chegar à mesa (REL-37). */
public record DelayedSlot(LocalDate day, int hour, int items, Duration averageWait) {
}
