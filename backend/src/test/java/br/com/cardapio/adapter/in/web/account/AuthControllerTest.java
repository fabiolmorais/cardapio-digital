package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AuthControllerTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    private MvcTestResult refresh(String refreshToken) {
        var request = mvc.post().uri("/api/v1/auth/refresh");
        if (refreshToken != null) {
            request = request.cookie(new Cookie("refresh_token", refreshToken));
        }
        return request.exchange();
    }

    /** Valor do cookie refresh_token gravado pela resposta. */
    private static String refreshCookie(MvcTestResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).as("cookie refresh_token na resposta").isNotNull();
        return cookie.getValue();
    }

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

    @Test
    void loginPutsTheRefreshTokenInAProtectedCookie() throws Exception {
        MvcTestResult login = login(signup(), PASSWORD);

        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("refresh_token=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=");
        // O refresh token nunca vai no corpo (o JavaScript não pode vê-lo).
        assertThat(login.getResponse().getContentAsString()).doesNotContain(refreshCookie(login));
    }

    @Test
    void refreshGivesANewAccessTokenAndRotatesTheCookie() throws Exception {
        MvcTestResult login = login(signup(), PASSWORD);

        MvcTestResult renewed = refresh(refreshCookie(login));

        assertThat(renewed).hasStatusOk();
        assertThat(refreshCookie(renewed)).isNotEqualTo(refreshCookie(login));
        MvcTestResult me = mvc.get().uri("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + json(renewed, "$.accessToken")).exchange();
        assertThat(me).hasStatusOk();
    }

    @Test
    void reusingAnOldRefreshTokenEndsTheSession() throws Exception {
        String first = refreshCookie(login(signup(), PASSWORD));
        String second = refreshCookie(refresh(first));

        assertThat(refresh(first)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(second)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshWithoutOrWithUnknownCookieGets401() {
        assertThat(refresh(null)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh("inventado")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutClearsTheCookieAndEndsTheSession() throws Exception {
        String token = refreshCookie(login(signup(), PASSWORD));

        MvcTestResult logout = mvc.post().uri("/api/v1/auth/logout").cookie(new Cookie("refresh_token", token)).exchange();

        assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(logout.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("refresh_token=", "Max-Age=0");
        assertThat(refresh(token)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Garçom desligado: o token de acesso dele morre em até 15 min, e a renovação é recusada na hora. */
    @Test
    void personRemovedFromTheRestaurantCannotRenew() throws Exception {
        MvcTestResult login = login(signup(), PASSWORD);
        jdbc.update("UPDATE membership SET active = false WHERE user_id = ?",
                UUID.fromString(json(login, "$.userId")));

        assertThat(refresh(refreshCookie(login))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Duas renovações ao mesmo tempo com o mesmo token (duas abas, ou o dono e um ladrão):
     * no máximo uma passa, e a sessão termina. Repete algumas vezes para as duas
     * requisições se cruzarem de verdade no banco.
     */
    @Test
    void twoRenewalsAtTheSameTimeWithTheSameTokenEndTheSession() throws Exception {
        for (int round = 0; round < 10; round++) {
            String token = refreshCookie(login(signup(), PASSWORD));
            CountDownLatch start = new CountDownLatch(1);
            Callable<MvcTestResult> renew = () -> {
                start.await();
                return refresh(token);
            };

            List<MvcTestResult> results;
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                Future<MvcTestResult> first = pool.submit(renew);
                Future<MvcTestResult> second = pool.submit(renew);
                start.countDown();
                results = List.of(first.get(), second.get());
            }

            List<MvcTestResult> accepted = results.stream()
                    .filter(r -> r.getResponse().getStatus() == HttpStatus.OK.value()).toList();
            assertThat(accepted).as("renovações aceitas na rodada %d", round).hasSizeLessThanOrEqualTo(1);
            for (MvcTestResult ok : accepted) {
                assertThat(refresh(refreshCookie(ok))).hasStatus(HttpStatus.UNAUTHORIZED);
            }
        }
    }
}