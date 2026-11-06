package br.com.cardapio.application.port.in.account;

/** O token que veio no link e a senha nova. */
public record ResetPasswordCommand(String token, String newPassword) {
}
