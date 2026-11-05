package br.com.cardapio.application.port.out.email;

/** Manda e-mails. Até a conta do Resend existir (Etapa 15), o adaptador só registra no log. */
public interface EmailSenderPort {

    void send(EmailMessage message);
}
