package br.com.cardapio.application.port.out.email;

import br.com.cardapio.domain.shared.model.Email;

import java.util.Objects;

/** Um e-mail de texto simples. */
public record EmailMessage(Email to, String subject, String text) {

    public EmailMessage {
        Objects.requireNonNull(to);
        Objects.requireNonNull(subject);
        Objects.requireNonNull(text);
    }
}
