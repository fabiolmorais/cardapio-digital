package br.com.cardapio.adapter.in.web.brand;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import br.com.cardapio.support.TestImages;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static br.com.cardapio.support.PrintedFiles.pdfText;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class BrandControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String BRAND = "/api/v1/establishment/brand";
    private static final String LOGO = "/api/v1/establishment/logo";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    private final HttpClient http = HttpClient.newHttpClient();

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    private Owner sakura;

    @BeforeEach
    void ownerOfSakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
    }

    private Owner newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD);
        assertThat(send("POST", null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            case "DELETE" -> mvc.delete().uri(uri);
            default -> mvc.get().uri(uri);
        };
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), path));
    }

    private static String brandJson(String name, String color, String theme, String instagram, String review) {
        return """
                {"name":"%s","color":"%s","theme":"%s","instagram":%s,"googleReviewUrl":%s}
                """.formatted(name, color, theme, instagram == null ? "null" : "\"" + instagram + "\"",
                review == null ? "null" : "\"" + review + "\"");
    }

    /** Os dois passos da logo, com o PUT direto no S3Mock no meio. */
    private MvcTestResult uploadLogo(String token, byte[] png) throws Exception {
        MvcTestResult upload = send("POST", token, LOGO + "/uploads",
                "{\"contentType\":\"image/png\",\"sizeBytes\":%d}".formatted(png.length));
        assertThat(upload).hasStatusOk();
        HttpRequest put = HttpRequest.newBuilder(URI.create(json(upload, "$.url")))
                .header("Content-Type", "image/png")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(png))
                .build();
        assertThat(http.send(put, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
        return send("POST", token, LOGO, "{\"uploadId\":\"%s\"}".formatted(json(upload, "$.uploadId")));
    }

    @Test
    void aNewRestaurantHasTheDefaultsAndThePalette() throws Exception {
        MvcTestResult brand = send("GET", sakura.token(), BRAND, null);

        assertThat(brand).hasStatusOk();
        assertThat(json(brand, "$.name")).isEqualTo("Sakura Sushi");
        assertThat(json(brand, "$.color")).isEqualTo("TOMATO");
        assertThat(json(brand, "$.theme")).isEqualTo("DARK");
        assertThat(json(brand, "$.logo")).isEqualTo("null");
        assertThat(brand).bodyJson().extractingPath("$.palette[*].color").asArray().hasSize(6);
        assertThat(brand).bodyJson().extractingPath("$.palette[0].primary").isEqualTo("#CC3F1C");
    }

    @Test
    void theFormSavesEverythingAndCleansTheInstagram() throws Exception {
        MvcTestResult saved = send("PUT", sakura.token(), BRAND,
                brandJson("Sakura", "PETROL", "LIGHT", "https://www.instagram.com/sakurasushi.sp/",
                        "https://g.page/r/CbX0exemplo/review"));

        assertThat(saved).hasStatusOk();
        assertThat(json(saved, "$.instagram")).isEqualTo("sakurasushi.sp");
        MvcTestResult read = send("GET", sakura.token(), BRAND, null);
        assertThat(json(read, "$.name")).isEqualTo("Sakura");
        assertThat(json(read, "$.color")).isEqualTo("PETROL");
        assertThat(json(read, "$.theme")).isEqualTo("LIGHT");
        assertThat(json(read, "$.googleReviewUrl")).isEqualTo("https://g.page/r/CbX0exemplo/review");
    }

    @Test
    void invalidDataGets400() throws Exception {
        MvcTestResult scam = send("PUT", sakura.token(), BRAND,
                brandJson("Sakura", "TOMATO", "DARK", null, "https://golpe.com/avalie"));
        assertThat(scam).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(scam, "$.detail")).startsWith("Use o link de avaliação do Google");

        assertThat(send("PUT", sakura.token(), BRAND, brandJson("Sakura", "ROSA", "DARK", null, null)))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("POST", sakura.token(), LOGO + "/uploads", "{\"contentType\":\"image/svg+xml\",\"sizeBytes\":100}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        MvcTestResult notImage = uploadLogo(sakura.token(), "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8));
        assertThat(notImage).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theLogoGoesToTheStorageAndToThePrintedDisplay() throws Exception {
        MvcTestResult withLogo = uploadLogo(sakura.token(), TestImages.encode("png", 300, 300));

        assertThat(withLogo).hasStatusOk();
        HttpResponse<byte[]> small = http.send(HttpRequest.newBuilder(URI.create(json(withLogo, "$.logo.small"))).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(TestImages.read(small.body()).getWidth()).isEqualTo(128);

        // O display impresso de uma mesa leva a logo.
        String sector = json(send("POST", sakura.token(), "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String table = json(send("POST", sakura.token(), "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.id");
        byte[] pdf = send("GET", sakura.token(), "/api/v1/tables/" + table + "/display?format=PDF", null)
                .getResponse().getContentAsByteArray();
        assertThat(pdfText(pdf, 1)).contains("SAKURA SUSHI", "Mesa 04");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("/Subtype/Image");

        // Sem logo, a página volta às iniciais.
        assertThat(json(send("DELETE", sakura.token(), LOGO, null), "$.logo")).isEqualTo("null");
    }

    @Test
    void waiterCannotChangeTheBrandAndAnonymousCannotSeeIt() {
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();

        assertThat(send("GET", waiter, BRAND, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("PUT", waiter, BRAND, brandJson("Meu", "TOMATO", "DARK", null, null)))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", null, BRAND, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Acesso entre restaurantes. Sem id na URL, a marca é sempre a do token: o outro dono só
     * enxerga e muda a dele, e o envio da logo da Sakura não serve para ele.
     */
    @Test
    void anotherOwnerOnlySeesAndChangesTheirOwnBrand() throws Exception {
        Owner other = newRestaurant("Outro Bar");
        MvcTestResult sakurasUpload = send("POST", sakura.token(), LOGO + "/uploads",
                "{\"contentType\":\"image/png\",\"sizeBytes\":100}");

        assertThat(json(send("GET", other.token(), BRAND, null), "$.name")).isEqualTo("Outro Bar");
        assertThat(send("PUT", other.token(), BRAND, brandJson("Invasor", "WINE", "LIGHT", null, null))).hasStatusOk();
        MvcTestResult stolen = send("POST", other.token(), LOGO,
                "{\"uploadId\":\"%s\"}".formatted(json(sakurasUpload, "$.uploadId")));
        assertThat(stolen).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

        MvcTestResult mine = send("GET", sakura.token(), BRAND, null);
        assertThat(json(mine, "$.name")).isEqualTo("Sakura Sushi");
        assertThat(json(mine, "$.color")).isEqualTo("TOMATO");
    }
}
