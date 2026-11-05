package br.com.cardapio.adapter.out.email;

import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.application.port.out.email.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Até a conta do Resend existir (Etapa 15), o e-mail só é registrado no log. Nunca o endereço
 * nem o texto: o log não guarda dado pessoal (CLAUDE.md, LGPD).
 */
@Component
class LogEmailSender implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("E-mail não enviado (o Resend entra na Etapa 15): \"{}\"", message.subject());
    }
}
