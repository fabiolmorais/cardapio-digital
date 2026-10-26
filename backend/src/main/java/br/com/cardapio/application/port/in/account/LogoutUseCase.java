package br.com.cardapio.application.port.in.account;

/**
 * Encerra a sessão do refresh token informado. Token ausente ou desconhecido não é erro.
 */
public interface LogoutUseCase {

    void logout(String refreshToken);
}
