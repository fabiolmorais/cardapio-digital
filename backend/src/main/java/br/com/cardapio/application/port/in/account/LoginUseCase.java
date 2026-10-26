package br.com.cardapio.application.port.in.account;

/**
 * Login de dono e gerente com e-mail e senha (RF02). Devolve o token de acesso.
 */
public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
