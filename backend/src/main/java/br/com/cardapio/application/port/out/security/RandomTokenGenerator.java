package br.com.cardapio.application.port.out.security;

/**
 * Gera um valor aleatório e imprevisível para tokens (refresh token, depois o token da mesa).
 */
public interface RandomTokenGenerator {

    String generate();
}
