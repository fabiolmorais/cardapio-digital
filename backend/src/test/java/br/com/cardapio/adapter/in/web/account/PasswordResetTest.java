package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.support.FakeResend;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** O "esqueci a senha" inteiro: o e-mail sai pelo Resend (um falso, na própria máquina). */
@IntegrationTest
class PasswordResetTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final Pattern TOKEN = Pattern.compile("redefinir-senha\\?token=([A-Za-z0-9_-]+)");
    private static final FakeResend RESEND = start();

    private static FakeResend start() {
        try {
            return new FakeResend();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void resendOn(DynamicPropertyRegistry registry) {
        registry.add("app.email.resend-api-key", () -> "re_teste");
        registry.add("app.email.resend-url", () -> RESEND.url().toString());
        registry.add("app.public-url", () -> "https://cardapio.test");
    }

    @Autowired
    MockMvcTester mvc;

    private String email;

    @BeforeEach
    void anOwner() {
        RESEND.received.clear();
        email = "dono-" + UUID.randomUUID() + "@teste.com";
        assertThat(post("/api/v1/signup", """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD))).hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult login(String password) {
        return post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private MvcTestResult confirm(String token, String password) {
        return post("/api/v1/auth/password-reset/confirm",
                "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, password));
    }

    /** Pede o link e tira o token do e-mail que chegou no Resend. */
    private String linkToken() throws Exception {
        assertThat(post("/api/v1/auth/password-reset", "{\"email\":\"%s\"}".formatted(email)))
                .hasStatus(HttpStatus.ACCEPTED);
        FakeResend.Received mail = RESEND.received.poll(5, TimeUnit.SECONDS);
        assertThat(mail).isNotNull();
        assertThat(JsonPath.<String>read(mail.json(), "$.to[0]")).isEqualTo(email);
        Matcher m = TOKEN.matcher(JsonPath.<String>read(mail.json(), "$.text"));
        assertThat(JsonPath.<String>read(mail.json(), "$.text")).contains("https://cardapio.test/redefinir-senha?token=");
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void theLinkChangesThePasswordAndLogsOutEverywhere() throws Exception {
        Cookie oldSession = login(PASSWORD).getResponse().getCookie("refresh_token");
        String token = linkToken();

        assertThat(confirm(token, "sushi2027novo")).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(login(PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login("sushi2027novo")).hasStatusOk();
        // A sessão aberta com a senha antiga não renova mais.
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(oldSession).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        // O link vale uma vez só.
        MvcTestResult again = confirm(token, "sushi2027outra");
        assertThat(again).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(JsonPath.<String>read(again.getResponse().getContentAsString(), "$.detail"))
                .isEqualTo("Este link para redefinir a senha expirou ou já foi usado. Peça outro.");
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAndNoEmail() throws Exception {
        assertThat(post("/api/v1/auth/password-reset", "{\"email\":\"ninguem-%s@teste.com\"}".formatted(UUID.randomUUID())))
                .hasStatus(HttpStatus.ACCEPTED);
        assertThat(RESEND.received.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void aWeakPasswordOrAnInventedTokenChangeNothing() throws Exception {
        String token = linkToken();

        assertThat(confirm(token, "curta")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(confirm("token-inventado", "sushi2027novo")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(login(PASSWORD)).hasStatusOk();
        // A senha fraca não gastou o link.
        assertThat(confirm(token, "sushi2027novo")).hasStatus(HttpStatus.NO_CONTENT);
    }
}
