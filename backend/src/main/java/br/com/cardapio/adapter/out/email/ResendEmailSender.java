package br.com.cardapio.adapter.out.email;

import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.application.port.out.email.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Manda o e-mail pela API do Resend (POST /emails), com o HttpClient do Java: sem SDK, sem
 * dependência nova. Sem a chave (desenvolvimento e testes), só registra o assunto no log.
 *
 * <p>Nunca registra o endereço nem o texto (CLAUDE.md, LGPD). Uma falha do Resend não derruba o
 * pedido de quem pediu o e-mail: fica no log (com o status), e a pessoa pode pedir de novo.
 */
@Component
@EnableConfigurationProperties(EmailProperties.class)
class ResendEmailSender implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final EmailProperties properties;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    ResendEmailSender(EmailProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    @Override
    public void send(EmailMessage message) {
        if (!properties.configured()) {
            log.info("E-mail não enviado (sem a chave do Resend): \"{}\"", message.subject());
            return;
        }
        String body = json.writeValueAsString(Map.of(
                "from", properties.from(),
                "to", List.of(message.to().value()),
                "subject", message.subject(),
                "text", message.text()));
        HttpRequest request = HttpRequest.newBuilder(properties.resendUrl().resolve("/emails"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + properties.resendApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                log.warn("E-mail \"{}\" recusado pelo Resend: {}", message.subject(), response.statusCode());
            }
        } catch (IOException e) {
            log.warn("E-mail \"{}\" não enviado: o Resend não respondeu", message.subject());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
