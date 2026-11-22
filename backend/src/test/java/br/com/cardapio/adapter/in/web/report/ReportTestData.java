package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os dados dos testes de relatório, montados direto no banco (é leitura: o caminho de escrita tem os
 * próprios testes): o restaurante pelo cadastro, e o salão, o cardápio e as comandas fechadas por SQL.
 */
final class ReportTestData {

    static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String PASSWORD = "sushi2026bom";

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    record Place(UUID sector, UUID table) {
    }

    record Item(UUID product, String name, long priceCents, int quantity, boolean canceled) {
    }

    /** Um item na cozinha: quanto levou até pronto (null = ainda não ficou pronto) e se foi cancelado. */
    record Prep(UUID product, String name, Duration time, boolean canceled) {
    }

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final AccessTokenIssuer tokenIssuer;

    ReportTestData(MockMvcTester mvc, JdbcTemplate jdbc, AccessTokenIssuer tokenIssuer) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.tokenIssuer = tokenIssuer;
    }

    Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON).content(signup).exchange())
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult login = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)).exchange();
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    /** Um setor com uma mesa. */
    Place place(Owner owner, String sectorName) {
        UUID sector = UUID.randomUUID();
        UUID table = UUID.randomUUID();
        jdbc.update("INSERT INTO sector (id, establishment_id, name, position) VALUES (?, ?, ?, 0)",
                sector, owner.establishmentId(), sectorName);
        jdbc.update("""
                INSERT INTO dining_table (id, establishment_id, sector_id, label, public_token, token_issued_at)
                VALUES (?, ?, ?, ?, ?, now())
                """, table, owner.establishmentId(), sector, sectorName.substring(0, 2) + "1", UUID.randomUUID().toString());
        return new Place(sector, table);
    }

    UUID category(Owner owner, String name) {
        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO menu_category (id, establishment_id, name, position) VALUES (?, ?, ?, 0)",
                category, owner.establishmentId(), name);
        return category;
    }

    /** Um produto numa categoria só dele, cadastrado agora. */
    UUID product(Owner owner, String name, long priceCents) {
        return product(owner, category(owner, "Cat " + name.substring(0, 4)), name, priceCents, Instant.now());
    }

    UUID product(Owner owner, UUID category, String name, long priceCents, Instant createdAt) {
        UUID product = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (id, establishment_id, category_id, name, price_cents, position, created_at)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                """, product, owner.establishmentId(), category, name, priceCents, Timestamp.from(createdAt));
        return product;
    }

    static Instant at(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), SAO_PAULO).toInstant();
    }

    static Item item(UUID product, String name, long price, int quantity) {
        return new Item(product, name, price, quantity, false);
    }

    static Item canceled(UUID product, String name, long price, int quantity) {
        return new Item(product, name, price, quantity, true);
    }

    /** Uma comanda fechada, com os itens, o pagamento (total + serviço) e quem fechou. */
    UUID closedTab(Owner owner, Place place, Instant opened, Instant closed, long totalCents, String method,
                   long serviceCents, Item... items) {
        UUID tab = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tab (id, establishment_id, table_id, status, opened_at, closed_at, closed_by, total_cents,
                                 payment_method, service_percent, service_accepted, service_cents, service_destination)
                VALUES (?, ?, ?, 'CLOSED', ?, ?, ?, ?, ?, 10, ?, ?, 'TEAM')
                """, tab, owner.establishmentId(), place.table(), Timestamp.from(opened), Timestamp.from(closed),
                owner.userId(), totalCents, method, serviceCents > 0, serviceCents);
        addItems(owner, place, tab, opened, items);
        if (totalCents + serviceCents == 0) {
            return tab; // conta zerada: o app não grava pagamento
        }
        jdbc.update("""
                INSERT INTO tab_payment (id, establishment_id, tab_id, amount_cents, payment_method, received_at,
                                         received_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), owner.establishmentId(), tab, totalCents + serviceCents, method,
                Timestamp.from(closed), owner.userId());
        return tab;
    }

    /** Uma comanda ainda aberta, com itens: não conta nos relatórios. */
    UUID openTab(Owner owner, Place place, Instant opened, Item... items) {
        UUID tab = UUID.randomUUID();
        jdbc.update("INSERT INTO tab (id, establishment_id, table_id, status, opened_at) VALUES (?, ?, ?, 'OPEN', ?)",
                tab, owner.establishmentId(), place.table(), Timestamp.from(opened));
        addItems(owner, place, tab, opened, items);
        return tab;
    }

    /** Uma comanda juntada em outra: não conta (o que ela tinha está na de destino). */
    void mergedTab(Owner owner, Place place, Instant opened, UUID into) {
        jdbc.update("""
                INSERT INTO tab (id, establishment_id, table_id, status, opened_at, merged_into_tab_id)
                VALUES (?, ?, ?, 'MERGED', ?, ?)
                """, UUID.randomUUID(), owner.establishmentId(), place.table(), Timestamp.from(opened), into);
    }

    private void addItems(Owner owner, Place place, UUID tab, Instant created, Item... items) {
        if (items.length == 0) {
            return;
        }
        UUID order = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, number, status, created_at, placed_by)
                VALUES (?, ?, ?, ?, 1, 'SENT', ?, ?)
                """, order, owner.establishmentId(), tab, place.table(), Timestamp.from(created), owner.userId());
        int position = 0;
        for (Item item : items) {
            jdbc.update("""
                    INSERT INTO order_item (id, establishment_id, order_id, position, product_id, product_name,
                                            unit_price_cents, quantity, status, created_at, canceled_at, canceled_by,
                                            cancel_reason, delivered_at, delivered_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), owner.establishmentId(), order, position++, item.product(), item.name(),
                    item.priceCents(), item.quantity(), item.canceled() ? "CANCELED" : "DELIVERED",
                    Timestamp.from(created),
                    item.canceled() ? Timestamp.from(created) : null, item.canceled() ? owner.userId() : null,
                    item.canceled() ? "Cliente desistiu" : null,
                    item.canceled() ? null : Timestamp.from(created), item.canceled() ? null : owner.userId());
        }
    }

    static Prep prep(UUID product, String name, int minutes) {
        return new Prep(product, name, Duration.ofMinutes(minutes), false);
    }

    /**
     * Um pedido com os itens na cozinha. Com {@code approved}, o garçom confirmou o
     * pedido nessa hora, e o preparo conta dali.
     */
    void kitchenOrder(Owner owner, Place place, Instant created, Instant approved, Prep... preps) {
        // Numa comanda já fechada: a mesa só pode ter uma aberta, e a cozinha não depende do fechamento.
        UUID tab = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tab (id, establishment_id, table_id, status, opened_at, closed_at, closed_by, total_cents,
                                 payment_method, service_percent, service_accepted, service_cents, service_destination)
                VALUES (?, ?, ?, 'CLOSED', ?, ?, ?, 0, 'PIX', 10, false, 0, 'TEAM')
                """, tab, owner.establishmentId(), place.table(), Timestamp.from(created),
                Timestamp.from(created.plus(Duration.ofHours(3))), owner.userId());
        UUID order = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, number, status, created_at, placed_by,
                                            approved_by, approved_at)
                VALUES (?, ?, ?, ?, 1, 'SENT', ?, ?, ?, ?)
                """, order, owner.establishmentId(), tab, place.table(), Timestamp.from(created), owner.userId(),
                approved == null ? null : owner.userId(), approved == null ? null : Timestamp.from(approved));
        Instant received = approved == null ? created : approved;
        int position = 0;
        for (Prep prep : preps) {
            Instant ready = prep.time() == null ? null : received.plus(prep.time());
            String status = prep.canceled() ? "CANCELED" : ready == null ? "PREPARING" : "READY";
            jdbc.update("""
                    INSERT INTO order_item (id, establishment_id, order_id, position, product_id, product_name,
                                            unit_price_cents, quantity, status, created_at, preparing_at, ready_at,
                                            canceled_at, canceled_by, cancel_reason)
                    VALUES (?, ?, ?, ?, ?, ?, 1000, 1, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), owner.establishmentId(), order, position++, prep.product(), prep.name(),
                    status, Timestamp.from(created), Timestamp.from(received),
                    ready == null ? null : Timestamp.from(ready),
                    prep.canceled() ? Timestamp.from(received.plus(Duration.ofHours(1))) : null,
                    prep.canceled() ? owner.userId() : null, prep.canceled() ? "Cliente desistiu" : null);
        }
    }

    /** Um celular na mesa (a sessão), aberto em {@code at}. */
    UUID session(Owner owner, Place place, Instant at) {
        UUID session = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO table_session (id, establishment_id, table_id, token_hash, created_at, last_seen_at,
                                           expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, session, owner.establishmentId(), place.table(), UUID.randomUUID().toString().replace("-", ""),
                Timestamp.from(at), Timestamp.from(at), Timestamp.from(at.plus(Duration.ofHours(2))));
        return session;
    }

    /** Um pedido feito pelo celular na comanda (sem itens: conta só como pedido). */
    void phoneOrder(Owner owner, Place place, UUID tab, UUID session, Instant at) {
        jdbc.update("""
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, session_id, number, status, created_at)
                VALUES (?, ?, ?, ?, ?, 2, 'SENT', ?)
                """, UUID.randomUUID(), owner.establishmentId(), tab, place.table(), session, Timestamp.from(at));
    }

    /** Um chamado do garçom, aceito e atendido por {@code by} em {@code accepted} (null: ainda esperando). */
    void call(Owner owner, Place place, UUID session, Instant created, Instant accepted, UUID by) {
        jdbc.update("""
                INSERT INTO call (id, establishment_id, table_id, session_id, type, status, created_at, accepted_at,
                                  accepted_by, done_at)
                VALUES (?, ?, ?, ?, 'WAITER', ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), owner.establishmentId(), place.table(), session,
                accepted == null ? "PENDING" : "DONE", Timestamp.from(created),
                accepted == null ? null : Timestamp.from(accepted), by,
                accepted == null ? null : Timestamp.from(accepted));
    }

    /** Alguém do salão, que entra pelo PIN. */
    UUID waiter(Owner owner, String name) {
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, name) VALUES (?, ?)", user, name);
        jdbc.update("""
                INSERT INTO membership (id, user_id, establishment_id, role, pin_hash) VALUES (?, ?, ?, 'STAFF', 'x')
                """, UUID.randomUUID(), user, owner.establishmentId());
        return user;
    }

    MvcTestResult get(Owner owner, String uri) {
        return get(owner.token(), uri);
    }

    MvcTestResult get(String token, String uri) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    static String json(MvcTestResult result, String path) throws Exception {
        Object value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value == null ? null : value.toString();
    }

    static <T> T read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    void onPlan(Owner owner, String code) {
        jdbc.update("""
                UPDATE subscription SET plan_id = (SELECT id FROM plan WHERE code = ?)
                WHERE organization_id = (SELECT organization_id FROM establishment WHERE id = ?)
                """, code, owner.establishmentId());
    }

    String token(Owner owner, Role role) {
        return tokenIssuer.issue(new TokenSubject(new UserId(owner.userId()), new EstablishmentId(owner.establishmentId()),
                role)).value();
    }
}
