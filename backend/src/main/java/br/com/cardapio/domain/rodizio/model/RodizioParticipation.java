package br.com.cardapio.domain.rodizio.model;

/**
 * Se este celular está no rodízio da mesa. O rodízio é da comanda, mas é cobrado por pessoa: o
 * celular que chega depois da ativação não entra sozinho, pergunta. Só quem está dentro pede rodadas.
 */
public enum RodizioParticipation {
    /** Chegou depois da ativação e ainda não disse se participa. */
    NONE,
    /** Disse que vai participar e chamou o garçom; espera ele incluir (ajustando as pessoas). */
    REQUESTED,
    /** Está no rodízio: estava na mesa na ativação, ou o garçom incluiu. */
    JOINED
}
