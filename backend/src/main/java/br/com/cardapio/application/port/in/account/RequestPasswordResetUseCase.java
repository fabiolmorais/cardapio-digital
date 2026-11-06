package br.com.cardapio.application.port.in.account;

public interface RequestPasswordResetUseCase {

    /**
     * Manda o link para o e-mail, se houver conta com senha nele. Não diz se havia: a resposta é
     * a mesma com ou sem conta (ninguém descobre quem é cliente pelo "esqueci a senha").
     */
    void request(String email);
}
