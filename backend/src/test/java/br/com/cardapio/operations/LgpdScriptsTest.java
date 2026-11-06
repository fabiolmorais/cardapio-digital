package br.com.cardapio.operations;

import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os scripts de LGPD (docs/operacao): os mesmos arquivos que rodamos com o psql, trocando a
 * variável :'slug'. Dois restaurantes: o pedido de um nunca mexe no outro.
 */
@IntegrationTest
class LgpdScriptsTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    record Restaurant(String email, String owner, String slug, String qr) {
    }

    private Restaurant sakura;
    private Restaurant tanuki;

    @BeforeEach
    void twoRestaurants() throws Exception {
        sakura = newRestaurant("Pastel do Carlos");
        tanuki = newRestaurant("Pastel da Ana");
    }

    private Restaurant newRestaurant(String waiter) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        MvcTestResult signup = send("POST", null, "/api/v1/signup", """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD));
        String owner = json(login(email), "$.accessToken");
        send("POST", owner, "/api/v1/team-members", "{\"name\":\"%s\",\"role\":\"STAFF\",\"pin\":\"258014\"}"
                .formatted(waiter));
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Pastéis\"}"), "$.id");
        send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Pastel de queijo","priceCents":1290,"featured":false,"available":true}
                """.formatted(category));
        return new Restaurant(email, owner, json(signup, "$.slug"), qr);
    }

    private MvcTestResult login(String email) {
        return send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = "POST".equals(method) ? mvc.post().uri(uri) : mvc.get().uri(uri);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    private static String script(String name) throws Exception {
        return Files.readString(Path.of("../docs/operacao/" + name));
    }

    /**
     * Roda o script inteiro (ele abre e fecha a própria transação). Se falhar no meio, desfaz: a
     * conexão volta para o pool limpa, e um erro aqui não derruba os outros testes.
     */
    private void run(String sql) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                try (Statement rollback = connection.createStatement()) {
                    rollback.execute("ROLLBACK");
                }
                throw e;
            }
            return null;
        });
    }

    private List<String> export(String slug) throws Exception {
        return jdbc.queryForList(script("exportar-dados.sql").replace(":'slug'", "?"), String.class, slug);
    }

    @Test
    void theExportBringsEverythingOfTheRestaurantAndNothingOfTheOther() throws Exception {
        String data = export(sakura.slug()).getFirst();

        assertThat(JsonPath.<String>read(data, "$.empresa.email")).isEqualTo(sakura.email());
        assertThat(JsonPath.<List<String>>read(data, "$.equipe[*].nome")).containsExactly("Marina", "Pastel do Carlos");
        assertThat(JsonPath.<List<String>>read(data, "$.mesas[*].mesa")).containsExactly("04");
        assertThat(JsonPath.<List<String>>read(data, "$.cardapio[0].produtos[*].nome")).containsExactly("Pastel de queijo");
        assertThat(JsonPath.<String>read(data, "$.assinatura.plano")).isEqualTo("Pro");
        assertThat(data).doesNotContain(tanuki.email()).doesNotContain("Pastel da Ana")
                .doesNotContain("password").doesNotContain("pin");
        assertThat(export("slug-que-nao-existe")).isEmpty();
    }

    @Test
    void anonymizingErasesThePeopleAndClosesTheRestaurantButNotTheOther() throws Exception {
        // O slug vem do cadastro (só letras, números e hífen): pode ir direto no texto do script.
        assertThat(sakura.slug()).matches("[a-z0-9-]+");
        run(script("anonimizar-restaurante.sql").replace(":'slug'", "'" + sakura.slug() + "'"));

        assertThat(login(sakura.email())).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(send("GET", null, "/api/v1/public/tables/" + sakura.qr(), null)).hasStatus(HttpStatus.NOT_FOUND);
        String data = export(sakura.slug()).getFirst();
        assertThat(data).doesNotContain(sakura.email()).doesNotContain("Marina").doesNotContain("Pastel do Carlos")
                .doesNotContain("98765");
        assertThat(JsonPath.<String>read(data, "$.empresa.documento")).isEqualTo("00000000000000");
        assertThat(JsonPath.<String>read(data, "$.restaurante.nome")).isEqualTo("Restaurante anonimizado");
        assertThat(JsonPath.<List<String>>read(data, "$.equipe[*].nome"))
                .containsExactly("Pessoa anonimizada", "Pessoa anonimizada");
        assertThat(JsonPath.<String>read(data, "$.assinatura.status")).isEqualTo("CANCELED");
        // O cardápio é dado do negócio, não da pessoa: fica.
        assertThat(JsonPath.<List<String>>read(data, "$.cardapio[0].produtos[*].nome")).containsExactly("Pastel de queijo");

        // O outro restaurante continua igual.
        assertThat(login(tanuki.email())).hasStatusOk();
        assertThat(send("GET", null, "/api/v1/public/tables/" + tanuki.qr(), null)).hasStatusOk();
        assertThat(export(tanuki.slug()).getFirst()).contains(tanuki.email()).contains("Pastel da Ana");
    }
}
