package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AuthControllerTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** Cria uma conta nova pela própria API e devolve o e-mail. */
    private String signup() {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String body = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON).content(body).exchange())
                .hasStatus(HttpStatus.CREATED);
        return email;
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)).exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    @Test
    void loginReturnsTokenThatOpensMe() throws Exception {
        String email = signup();

        MvcTestResult login = login(email.toUpperCase(), PASSWORD);
        assertThat(login).hasStatusOk();
        assertThat(login).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(login).bodyJson().extractingPath("$.establishment.role").isEqualTo("OWNER");

        MvcTestResult me = mvc.get().uri("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + json(login, "$.accessToken")).exchange();
        assertThat(me).hasStatusOk();
        assertThat(me).bodyJson().extractingPath("$.email").isEqualTo(email);
        assertThat(me).bodyJson().extractingPath("$.establishment.id")
                .isEqualTo(json(login, "$.establishment.id"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSame401() throws Exception {
        String email = signup();

        MvcTestResult wrongPassword = login(email, "senhaErrada123");
        MvcTestResult unknownEmail = login("ninguem-" + UUID.randomUUID() + "@teste.com", PASSWORD);

        assertThat(wrongPassword).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(wrongPassword, "$.detail")).isEqualTo(json(unknownEmail, "$.detail"));
    }

    @Test
    void meRequiresAValidToken() {
        assertThat(mvc.get().uri("/api/v1/me").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer nao.e.um-token").exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = json(login(signup(), PASSWORD), "$.accessToken");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(mvc.get().uri("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Acesso entre restaurantes: mesmo com um token assinado de verdade, quem não trabalha no
     * estabelecimento do token não vê nada dele.
     */
    @Test
    void tokenForAnotherRestaurantGets401() throws Exception {
        MvcTestResult marina = login(signup(), PASSWORD);
        MvcTestResult otherOwner = login(signup(), PASSWORD);

        UserId marinaId = new UserId(UUID.fromString(json(marina, "$.userId")));
        EstablishmentId otherRestaurant = new EstablishmentId(UUID.fromString(json(otherOwner, "$.establishment.id")));
        String forged = tokenIssuer.issue(new TokenSubject(marinaId, otherRestaurant, Role.OWNER)).value();

        assertThat(mvc.get().uri("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

}