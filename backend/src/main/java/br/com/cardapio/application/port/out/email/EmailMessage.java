package br.com.cardapio.application.port.out.email;

import br.com.cardapio.domain.shared.model.Email;

import java.util.Objects;

/**
 * Um e-mail. O texto simples vai sempre (é o que aparece em quem não mostra HTML); o HTML é opcional.
 *
 * @param html null = só texto
 */
public record EmailMessage(Email to, String subject, String text, String html) {

    public EmailMessage {
        Objects.requireNonNull(to);
        Objects.requireNonNull(subject);
        Objects.requireNonNull(text);
    }

    public EmailMessage(Email to, String subject, String text) {
        this(to, subject, text, null);
    }
}
