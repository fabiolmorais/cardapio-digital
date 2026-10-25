package br.com.cardapio.application.port.in.account;

public record SignupCommand(
        String establishmentName,
        String ownerName,
        String document,
        String whatsapp,
        String email,
        String password
) {

    @Override
    public String toString() {
        // Nunca expor dados pessoais nem a senha em logs.
        return "SignupCommand[establishmentName=" + establishmentName + "]";
    }
}
