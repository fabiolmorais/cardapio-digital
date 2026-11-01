package br.com.cardapio.adapter.in.web.menu;

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

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ProductMediaControllerTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    private final HttpClient http = HttpClient.newHttpClient();

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    private Owner sakura;
    private String guioza;

    @BeforeEach
    void productOfSakura() throws Exception {
        sakura = newRestaurant();
        guioza = product(sakura.token());
    }

    private Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(send("POST", null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    private String product(String token) throws Exception {
        String category = json(send("POST", token, "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        MvcTestResult created = send("POST", token, "/api/v1/products", """
                {"categoryId":"%s","name":"Guioza","priceCents":2990,"featured":false,"available":true}
                """.formatted(category));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
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
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    private static String media(String productId) {
        return "/api/v1/products/" + productId + "/media";
    }

    /** Passo 1: pede a URL assinada. */
    private MvcTestResult startUpload(String token, String productId, String contentType, long size) {
        return send("POST", token, media(productId) + "/uploads",
                "{\"contentType\":\"%s\",\"sizeBytes\":%d}".formatted(contentType, size));
    }

    /** Faz o papel do navegador: PUT direto no armazenamento, sem passar pelo back. */
    private int browserPut(MvcTestResult upload, byte[] content) throws Exception {
        HttpRequest put = HttpRequest.newBuilder(URI.create(json(upload, "$.url")))
                .header("Content-Type", json(upload, "$.headers['Content-Type']"))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        return http.send(put, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** Os dois passos e o envio no meio. */
    private MvcTestResult uploadPhoto(String token, String productId, byte[] content) throws Exception {
        MvcTestResult upload = startUpload(token, productId, "image/jpeg", content.length);
        assertThat(upload).hasStatusOk();
        assertThat(browserPut(upload, content)).isEqualTo(200);
        return send("POST", token, media(productId), "{\"uploadId\":\"%s\"}".formatted(json(upload, "$.uploadId")));
    }

    private BufferedImage download(String url) throws Exception {
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);
        return TestImages.read(response.body());
    }

    @Test
    void ownerUploadsAPhotoAndGetsTheThreeSizes() throws Exception {
        MvcTestResult added = uploadPhoto(sakura.token(), guioza, TestImages.encode("jpeg", 2400, 1600));

        assertThat(added).hasStatus(HttpStatus.CREATED);
        assertThat(added).bodyJson().extractingPath("$.width").isEqualTo(1440);
        assertThat(added).bodyJson().extractingPath("$.height").isEqualTo(960);
        assertThat(download(json(added, "$.urls.large")).getWidth()).isEqualTo(1440);
        assertThat(download(json(added, "$.urls.medium")).getWidth()).isEqualTo(720);
        assertThat(download(json(added, "$.urls.thumb")).getHeight()).isEqualTo(240);

        assertThat(send("GET", sakura.token(), media(guioza), null))
                .bodyJson().extractingPath("$.content[0].id").isEqualTo(json(added, "$.id"));
    }

    @Test
    void whatTheBrowserSaysIsCheckedTwice() throws Exception {
        // Antes do envio: o que o navegador diz.
        MvcTestResult gif = startUpload(sakura.token(), guioza, "image/gif", 1000);
        assertThat(gif).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(gif, "$.detail")).isEqualTo("Envie uma foto JPG ou PNG");
        assertThat(startUpload(sakura.token(), guioza, "image/jpeg", 9L * 1024 * 1024))
                .hasStatus(HttpStatus.BAD_REQUEST);

        // Depois do envio: o que o arquivo é de verdade.
        MvcTestResult fake = uploadPhoto(sakura.token(), guioza, "<html>não é foto</html>".getBytes(StandardCharsets.UTF_8));
        assertThat(fake).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(fake, "$.detail")).isEqualTo("O arquivo não é uma foto JPG ou PNG");

        // Avisar sem ter enviado.
        MvcTestResult upload = startUpload(sakura.token(), guioza, "image/jpeg", 1000);
        MvcTestResult missing = send("POST", sakura.token(), media(guioza),
                "{\"uploadId\":\"%s\"}".formatted(json(upload, "$.uploadId")));
        assertThat(missing).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(missing, "$.detail")).isEqualTo("A foto não chegou. Envie de novo.");
    }

    @Test
    void reorderAndRemove() throws Exception {
        byte[] photo = TestImages.encode("png", 400, 300);
        String first = json(uploadPhoto(sakura.token(), guioza, photo), "$.id");
        String second = json(uploadPhoto(sakura.token(), guioza, photo), "$.id");

        assertThat(send("PUT", sakura.token(), "/api/v1/products/" + guioza + "/media-order",
                "{\"ids\":[\"%s\",\"%s\"]}".formatted(second, first))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", sakura.token(), media(guioza), null))
                .bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(second, first);

        assertThat(send("DELETE", sakura.token(), media(guioza) + "/" + second, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", sakura.token(), media(guioza), null))
                .bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(first);
    }

    @Test
    void waiterCannotManagePhotos() {
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();

        assertThat(startUpload(waiter, guioza, "image/jpeg", 1000)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", waiter, media(guioza), null)).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Acesso entre restaurantes: o dono de outro restaurante não vê nem mexe nas fotos da Sakura. */
    @Test
    void ownerOfAnotherRestaurantGets404AndChangesNothing() throws Exception {
        String photo = json(uploadPhoto(sakura.token(), guioza, TestImages.encode("jpeg", 400, 300)), "$.id");
        Owner other = newRestaurant();
        String theirs = product(other.token());

        assertThat(startUpload(other.token(), guioza, "image/jpeg", 1000)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.token(), media(guioza), null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", other.token(), media(guioza), "{\"uploadId\":\"%s\"}".formatted(UUID.randomUUID())))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.token(), media(guioza) + "/" + photo, null)).hasStatus(HttpStatus.NOT_FOUND);
        // A foto da Sakura pela URL do produto dele também não.
        assertThat(send("DELETE", other.token(), media(theirs) + "/" + photo, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PUT", other.token(), "/api/v1/products/" + guioza + "/media-order",
                "{\"ids\":[\"%s\"]}".formatted(photo))).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(send("GET", sakura.token(), media(guioza), null))
                .bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(photo);
    }
}
