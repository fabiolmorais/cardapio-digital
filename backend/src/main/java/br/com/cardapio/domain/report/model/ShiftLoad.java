package br.com.cardapio.domain.report.model;

import java.time.LocalDate;

/** Uma hora de um dia: quantos chamados e quantas pessoas do salão estavam no turno (REL-53). */
public record ShiftLoad(LocalDate day, int hour, int calls, int waiters) {

    public double callsPerWaiter() {
        return waiters == 0 ? 0 : (double) calls / waiters;
    }
}
