package br.com.cardapio.domain.device.model;

/** Por que o turno de quem entrou pelo PIN acabou (REL-53). */
public enum ShiftEnd {
    /** A pessoa saiu do app. */
    LOGOUT,
    /** Outra pessoa entrou com o PIN no mesmo aparelho. */
    REPLACED,
    /** O aparelho foi desvinculado do restaurante. */
    UNLINKED,
    /** A sessão venceu sem uso (ou foi revogada, por exemplo, ao desligar a pessoa). */
    EXPIRED
}
