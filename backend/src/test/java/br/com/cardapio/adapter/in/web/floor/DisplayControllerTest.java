package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static br.com.cardapio.support.PrintedFiles.pdfPages;
import static br.com.cardapio.support.PrintedFiles.pdfText;
import static br.com.cardapio.support.PrintedFiles.readQr;
import static org.assertj.core.api.Assertions.assertThat;

/** O caminho inteiro: baixar o display, ler o QR e abrir a página pública da mesa com ele. */
@IntegrationTest
class DisplayControllerTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    record Owner(String token, String establishmentName) {
    }

    private Owner sakura;
    private String salao;
    private String table04;

    @BeforeEach
    void restaurantWithATable() throws Exception {
        sakura = newRestaurant();
        salao = id(post(sakura.token(), "/api/v1/sectors", "{\"name\":\"Salão principal\"}"));
        table04 = id(post(sakura.token(), "/api/v1/tables", "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(salao)));
    }

    private Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String name = "Restaurante " + UUID.randomUUID().toString().substring(0, 8);
        String signup = """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD);
        assertThat(post(null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = post(null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken"), name);
    }

    private MvcTestResult post(String token, String uri, String body) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).exchange();
    }

    private MvcTestResult get(String token, String uri) {
        var request = mvc.get().uri(uri);
        return (token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).exchange();
    }

    private static String id(MvcTestResult created) throws Exception {
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    /** O token da mesa: o que vem depois de /m/ na URL do QR. */
    private static String tokenOf(String url) {
        return url.substring(url.lastIndexOf("/m/") + 3);
    }

    @Test
    void scanningThePrintedQrOpensTheRightTable() throws Exception {
        MvcTestResult png = get(sakura.token(), "/api/v1/tables/" + table04 + "/display?format=PNG");

        assertThat(png).hasStatusOk();
        assertThat(png.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(png.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").contains("mesa-04.png");
        assertThat(png.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        // O QR aponta para {app.public-url}/m/{token}; com ele, a página pública acha a mesa.
        String url = readQr(png.getResponse().getContentAsByteArray());
        assertThat(url).startsWith("http://localhost:3000/m/");
        MvcTestResult page = get(null, "/api/v1/public/tables/" + tokenOf(url));
        assertThat(page).hasStatusOk();
        assertThat(page).bodyJson().extractingPath("$.tableLabel").isEqualTo("04");
        assertThat(page).bodyJson().extractingPath("$.establishmentName").isEqualTo(sakura.establishmentName());
    }

    /** O front (outra origem) precisa ler o nome do arquivo: o CORS expõe o Content-Disposition. */
    @Test
    void theFrontCanReadTheFileName() {
        MvcTestResult png = mvc.get().uri("/api/v1/tables/" + table04 + "/display?format=PNG")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.token()).exchange();

        assertThat(png).hasStatusOk();
        assertThat(png.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("Content-Disposition");
    }

    @Test
    void svgAndPdfOfATable() throws Exception {
        MvcTestResult svg = get(sakura.token(), "/api/v1/tables/" + table04 + "/display?format=SVG");
        assertThat(svg.getResponse().getContentType()).isEqualTo("image/svg+xml");
        assertThat(svg.getResponse().getContentAsString()).startsWith("<svg");

        MvcTestResult pdf = get(sakura.token(), "/api/v1/tables/" + table04 + "/display");
        assertThat(pdf.getResponse().getContentType()).isEqualTo("application/pdf");
        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertThat(pdfPages(bytes)).isEqualTo(1);
        assertThat(pdfText(bytes, 1)).contains(sakura.establishmentName().toUpperCase(), "Mesa 04");
    }

    @Test
    void sectorPdfHasAPagePerActiveTable() throws Exception {
        id(post(sakura.token(), "/api/v1/tables", "{\"label\":\"05\",\"sectorId\":\"%s\"}".formatted(salao)));

        MvcTestResult pdf = get(sakura.token(), "/api/v1/sectors/" + salao + "/displays");

        assertThat(pdf).hasStatusOk();
        assertThat(pdf.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("setor-salao-principal.pdf");
        assertThat(pdfPages(pdf.getResponse().getContentAsByteArray())).isEqualTo(2);
    }

    @Test
    void emptySectorHasNothingToPrint() throws Exception {
        String deck = id(post(sakura.token(), "/api/v1/sectors", "{\"name\":\"Deck\"}"));

        assertThat(get(sakura.token(), "/api/v1/sectors/" + deck + "/displays"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void rotatedOrDeactivatedTableNoLongerOpens() throws Exception {
        String url = readQr(get(sakura.token(), "/api/v1/tables/" + table04 + "/display?format=PNG")
                .getResponse().getContentAsByteArray());

        assertThat(post(sakura.token(), "/api/v1/tables/" + table04 + "/token-rotation", "")).hasStatusOk();
        assertThat(get(null, "/api/v1/public/tables/" + tokenOf(url))).hasStatus(HttpStatus.NOT_FOUND);

        String newUrl = readQr(get(sakura.token(), "/api/v1/tables/" + table04 + "/display?format=PNG")
                .getResponse().getContentAsByteArray());
        assertThat(get(null, "/api/v1/public/tables/" + tokenOf(newUrl))).hasStatusOk();

        assertThat(mvc.patch().uri("/api/v1/tables/" + table04).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}").header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.token()).exchange())
                .hasStatusOk();
        assertThat(get(null, "/api/v1/public/tables/" + tokenOf(newUrl))).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void inventedTokensGet404() {
        assertThat(get(null, "/api/v1/public/tables/token-que-ninguem-gerou-000")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(null, "/api/v1/public/tables/x")).hasStatus(HttpStatus.NOT_FOUND);
    }

    /** Acesso entre restaurantes: o dono de outro restaurante não baixa o QR (nem o token) da mesa da Sakura. */
    @Test
    void ownerOfAnotherRestaurantGets404() throws Exception {
        Owner other = newRestaurant();

        assertThat(get(other.token(), "/api/v1/tables/" + table04 + "/display?format=PNG")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(other.token(), "/api/v1/sectors/" + salao + "/displays")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadsNeedLoginAndAWrongFormatIs400() {
        assertThat(get(null, "/api/v1/tables/" + table04 + "/display")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(get(sakura.token(), "/api/v1/tables/" + table04 + "/display?format=GIF"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
