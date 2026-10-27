package br.com.cardapio.application.port.in.account;

public interface PinLoginUseCase {

    /** Entra com PIN num aparelho vinculado; devolve a mesma sessão do login por e-mail. */
    LoginResult login(PinLoginCommand command);
}
