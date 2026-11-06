package br.com.cardapio.adapter.out.email;

import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.support.FakeResend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ResendEmailSenderTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final FakeResend resend = new FakeResend();
    private final EmailMessage message = new EmailMessage(new Email("marina@sakura.com"), "Redefina sua senha",
            "Abra o link: https://app.test/redefinir-senha?token=abc");

    ResendEmailSenderTest() throws IOException {
    }

    @AfterEach
    void stop() {
        resend.close();
    }

    private ResendEmailSender sender(String apiKey) {
        return new ResendEmailSender(new EmailProperties(apiKey, "Cardápio <avisos@cardapio.test>", resend.url()), json);
    }

    @Test
    void sendsTheMessageToTheResendApiWithTheKey() throws Exception {
        sender("re_teste").send(message);

        FakeResend.Received got = resend.received.poll(5, TimeUnit.SECONDS);
        assertThat(got).isNotNull();
        assertThat(got.path()).isEqualTo("/emails");
        assertThat(got.authorization()).isEqualTo("Bearer re_teste");
        JsonNode body = json.readTree(got.json());
        assertThat(body.get("from").asString()).isEqualTo("Cardápio <avisos@cardapio.test>");
        assertThat(body.get("to").get(0).asString()).isEqualTo("marina@sakura.com");
        assertThat(body.get("subject").asString()).isEqualTo("Redefina sua senha");
        assertThat(body.get("text").asString()).contains("token=abc");
    }

    @Test
    void withoutTheKeyNothingLeavesAndAFailureDoesNotBreakTheRequest() throws Exception {
        sender("").send(message);
        assertThat(resend.received.poll(300, TimeUnit.MILLISECONDS)).isNull();

        resend.status = 500;
        sender("re_teste").send(message); // só fica no log
        assertThat(resend.received.poll(5, TimeUnit.SECONDS)).isNotNull();
    }
}
