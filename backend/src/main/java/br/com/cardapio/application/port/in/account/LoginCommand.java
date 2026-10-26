package br.com.cardapio.application.port.in.account;

public record LoginCommand(String email, String password) {

    @Override
    public String toString() {
        return "LoginCommand[protegido]";
    }
}
