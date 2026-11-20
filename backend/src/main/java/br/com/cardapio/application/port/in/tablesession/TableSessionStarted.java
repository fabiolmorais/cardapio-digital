package br.com.cardapio.application.port.in.tablesession;

import java.time.Instant;

/**
 * A sessão em uso. O token cru vai SÓ para o cookie (o controller o coloca lá); o banco tem só o hash.
 *
 * @param resumed true quando o celular já tinha uma sessão válida nesta mesa
 * @param movedTo o QR da mesa para onde a comanda deste celular foi (ADR 0012): a página vai para lá,
 *                sem abrir sessão nesta mesa; null no caso comum
 */
public record TableSessionStarted(String token, Instant expiresAt, boolean resumed, String movedTo) {

    public TableSessionStarted(String token, Instant expiresAt, boolean resumed) {
        this(token, expiresAt, resumed, null);
    }
}
