package br.com.cardapio.application.port.in.account;

public interface SignupUseCase {

    SignupResult signup(SignupCommand command);
}
