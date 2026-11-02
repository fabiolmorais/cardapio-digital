package br.com.cardapio.application.port.in.tablesession;

import java.time.Instant;

/**
 * A sessão em uso. O token cru vai SÓ para o cookie (o controller o coloca lá); o banco tem só o hash.
 *
 * @param resumed true quando o celular já tinha uma sessão válida nesta mesa
 */
public record TableSessionStarted(String token, Instant expiresAt, boolean resumed) {
}
