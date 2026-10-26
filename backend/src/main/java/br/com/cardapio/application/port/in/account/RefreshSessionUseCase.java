package br.com.cardapio.application.port.in.account;

/**
 * Troca um refresh token válido por um token de acesso novo e um refresh token novo.
 */
public interface RefreshSessionUseCase {

    LoginResult refresh(String refreshToken);
}
