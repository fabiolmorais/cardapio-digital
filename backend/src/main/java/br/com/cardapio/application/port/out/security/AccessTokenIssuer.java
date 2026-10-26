package br.com.cardapio.application.port.out.security;

/**
 * Emite o token de acesso. A implementação (JWT assinado) é detalhe de infraestrutura.
 */
public interface AccessTokenIssuer {

    AccessToken issue(TokenSubject subject);
}
