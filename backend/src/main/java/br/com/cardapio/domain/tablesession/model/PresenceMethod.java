package br.com.cardapio.domain.tablesession.model;

/** Como a sessão provou que o celular está no salão (a equipe vê isso no painel). */
public enum PresenceMethod {
    /** O IP do celular é o da internet do restaurante. */
    WIFI,
    /** A localização do celular caiu dentro do raio. */
    GPS,
    /** Um garçom liberou a mesa. */
    STAFF
}
