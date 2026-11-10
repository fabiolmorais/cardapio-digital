package br.com.cardapio.adapter.out.persistence.account;

import java.time.Instant;

/** A pessoa da lista da equipe, com o último acesso (a sessão mais nova neste restaurante). */
public interface TeamMemberAccessRow extends TeamMemberRow {

    Instant getLastAccessAt();
}
