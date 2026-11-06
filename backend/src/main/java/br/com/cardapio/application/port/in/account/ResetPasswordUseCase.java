package br.com.cardapio.application.port.in.account;

public interface ResetPasswordUseCase {

    void reset(ResetPasswordCommand command);
}
