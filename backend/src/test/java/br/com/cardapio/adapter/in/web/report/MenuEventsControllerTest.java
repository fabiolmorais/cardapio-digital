package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.in.menu.PurgeMenuEventsUseCase;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.menu.model.MenuEvent;
import br.com.cardapio.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Os eventos do cardápio (C2): o celular da mesa conta o que abriu, viu e pôs no carrinho. */
@IntegrationTest
class MenuEventsControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PurgeMenuEventsUseCase purge;

    private ReportTestData data;
    private Owner sakura;
    private String qr;
    private Cookie phone;
    private UUID temaki;

    @BeforeEach
    void aPhoneAtTheTable() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        Place salao = data.place(sakura, "Salão");
        temaki = data.product(sakura, "Temaki", 2500);
        qr = jdbc.queryForObject("SELECT public_token FROM dining_table WHERE id = ?", String.class, salao.table());
        phone = mvc.post().uri("/api/v1/public/tables/" + qr + "/session").exchange().getResponse()
                .getCookie("table_session");
        assertThat(phone).isNotNull();
    }

    private MvcTestResult send(String body, MediaType type, Cookie cookie) {
        var request = mvc.post().uri("/api/v1/public/tables/" + qr + "/events").contentType(type).content(body);
        return cookie == null ? request.exchange() : request.cookie(cookie).exchange();
    }

    private int events(String kind) {
        return jdbc.queryForObject("SELECT count(*) FROM menu_event WHERE establishment_id = ? AND kind = ?",
                Integer.class, sakura.establishmentId(), kind);
    }

    @Test
    void thePhoneCountsWhatItOpenedSawAndAdded() {
        MvcTestResult result = send("""
                {"events":[{"kind":"PRODUCT_VIEW","productId":"%1$s"},{"kind":"REEL_VIEW","productId":"%1$s"},
                           {"kind":"CART_ADD","productId":"%1$s","quantity":3}]}
                """.formatted(temaki), MediaType.APPLICATION_JSON, phone);

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        assertThat(events("PRODUCT_VIEW")).isEqualTo(1);
        assertThat(events("REEL_VIEW")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT quantity FROM menu_event WHERE kind = 'CART_ADD' AND product_id = ?",
                Integer.class, temaki)).isEqualTo(3);
    }

    @Test
    void theBeaconSendsItAsPlainText() {
        // O sendBeacon manda texto, para não precisar da consulta prévia do CORS.
        assertThat(send("""
                {"events":[{"kind":"PRODUCT_VIEW","productId":"%s"}]}
                """.formatted(temaki), MediaType.TEXT_PLAIN, phone)).hasStatus(HttpStatus.ACCEPTED);
        assertThat(events("PRODUCT_VIEW")).isEqualTo(1);
    }

    @Test
    void withoutTheTableSessionNothingIsRecorded() {
        assertThat(send("""
                {"events":[{"kind":"PRODUCT_VIEW","productId":"%s"}]}
                """.formatted(temaki), MediaType.APPLICATION_JSON, null)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(events("PRODUCT_VIEW")).isZero();
    }

    @Test
    void invalidBatchesAreRefused() {
        String tooMany = IntStream.range(0, MenuEvent.MAX_BATCH + 1)
                .mapToObj(i -> "{\"kind\":\"PRODUCT_VIEW\",\"productId\":\"" + temaki + "\"}")
                .collect(Collectors.joining(",", "{\"events\":[", "]}"));

        assertThat(send(tooMany, MediaType.APPLICATION_JSON, phone)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("isto não é JSON", MediaType.TEXT_PLAIN, phone)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("""
                {"events":[{"kind":"CART_ADD","productId":"%s","quantity":99}]}
                """.formatted(temaki), MediaType.APPLICATION_JSON, phone)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("""
                {"events":[{"kind":"BUY_NOW","productId":"%s"}]}
                """.formatted(temaki), MediaType.APPLICATION_JSON, phone)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(events("PRODUCT_VIEW") + events("CART_ADD")).isZero();
    }

    @Test
    void aProductOfAnotherRestaurantIsIgnored() throws Exception {
        Owner other = data.newRestaurant();
        UUID theirs = data.product(other, "Lamen", 3000);

        assertThat(send("""
                {"events":[{"kind":"PRODUCT_VIEW","productId":"%s"},{"kind":"PRODUCT_VIEW","productId":"%s"}]}
                """.formatted(theirs, UUID.randomUUID()), MediaType.APPLICATION_JSON, phone))
                .hasStatus(HttpStatus.ACCEPTED);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM menu_event WHERE product_id = ?", Integer.class, theirs))
                .isZero();
        assertThat(events("PRODUCT_VIEW")).isZero();
    }

    @Test
    void aSessionStopsRecordingAfterTheLimit() {
        UUID session = jdbc.queryForObject("SELECT id FROM table_session WHERE establishment_id = ?", UUID.class,
                sakura.establishmentId());
        jdbc.update("""
                INSERT INTO menu_event (id, establishment_id, session_id, product_id, kind, quantity, occurred_at)
                SELECT gen_random_uuid(), ?, ?, ?, 'PRODUCT_VIEW', 1, now() FROM generate_series(1, ?)
                """, sakura.establishmentId(), session, temaki, MenuEvent.MAX_PER_SESSION - 1);

        assertThat(send("""
                {"events":[{"kind":"REEL_VIEW","productId":"%1$s"},{"kind":"REEL_VIEW","productId":"%1$s"}]}
                """.formatted(temaki), MediaType.APPLICATION_JSON, phone)).hasStatus(HttpStatus.ACCEPTED);

        assertThat(events("REEL_VIEW")).isEqualTo(1);
    }

    @Test
    void eventsOlderThan13MonthsAreDeleted() {
        UUID session = UUID.randomUUID();
        for (int days : new int[] {10, MenuEvent.RETENTION_DAYS + 1}) {
            jdbc.update("""
                    INSERT INTO menu_event (id, establishment_id, session_id, product_id, kind, quantity, occurred_at)
                    VALUES (?, ?, ?, ?, 'PRODUCT_VIEW', 1, ?)
                    """, UUID.randomUUID(), sakura.establishmentId(), session, temaki,
                    Timestamp.from(Instant.now().minus(Duration.ofDays(days))));
        }

        assertThat(purge.purge()).isGreaterThanOrEqualTo(1);

        assertThat(events("PRODUCT_VIEW")).isEqualTo(1);
    }
}
