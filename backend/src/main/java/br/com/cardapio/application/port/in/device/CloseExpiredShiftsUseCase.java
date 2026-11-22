package br.com.cardapio.application.port.in.device;

/** Fecha os turnos cuja sessão venceu (a tarefa agendada). */
public interface CloseExpiredShiftsUseCase {

    /** Quantos turnos fechou. */
    int closeExpired();
}
