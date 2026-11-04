package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import br.com.cardapio.support.TestcontainersConfiguration;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O vídeo do prato de ponta a ponta, com o serviço de vídeo falso: o envio TUS em pedaços, a
 * galeria que vê o vídeo pronto, o vídeo tocando, o aviso (webhook) e a separação entre restaurantes.
 */
@IntegrationTest
class VideoFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Restaurant(String owner, UUID userId, UUID establishmentId, String product) {

        String videos() {
            return "/api/v1/products/" + product + "/videos";
        }

        String media() {
            return "/api/v1/products/" + product + "/media";
        }
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = json(login, "$.accessToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        String product = json(send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Guioza","priceCents":2990,"featured":false,"available":true}
                """.formatted(category)), "$.id");
        return new Restaurant(owner, UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")), product);
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
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

    /** Pede o envio de um vídeo de `size` bytes; devolve a resposta (mediaId, endpoint e cabeçalhos). */
    private MvcTestResult startVideo(Restaurant r, long size) {
        return send("POST", r.owner(), r.videos(), "{\"contentType\":\"video/mp4\",\"sizeBytes\":%d}".formatted(size));
    }

    /** Faz o papel do navegador: o POST de criação do TUS, com os cabeçalhos que o back deu. */
    private MvcTestResult tusCreate(MvcTestResult started, long length) throws Exception {
        Map<String, String> headers = JsonPath.read(started.getResponse().getContentAsString(), "$.headers");
        var request = mvc.post().uri(URI.create(json(started, "$.endpoint")).getPath())
                .header("Tus-Resumable", "1.0.0")
                .header("Upload-Length", String.valueOf(length))
                .header("Upload-Metadata", "filetype " + Base64.getEncoder()
                        .encodeToString("video/mp4".getBytes(StandardCharsets.UTF_8)));
        for (var header : headers.entrySet()) {
            request = request.header(header.getKey(), header.getValue());
        }
        return request.exchange();
    }

    private MvcTestResult tusPatch(String location, long offset, byte[] chunk) {
        return mvc.patch().uri(location).header("Tus-Resumable", "1.0.0").header("Upload-Offset", offset)
                .contentType("application/offset+octet-stream").content(chunk).exchange();
    }

    private MvcTestResult webhook(String token, String videoId) {
        return mvc.post().uri("/api/v1/webhooks/video?token=" + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"VideoLibraryId\":1,\"VideoGuid\":\"%s\",\"Status\":3}".formatted(videoId)).exchange();
    }

    @Test
    void theOwnerSendsAVideoInPiecesAndItShowsUpReady() throws Exception {
        byte[] video = "um vídeo de mentira".getBytes(StandardCharsets.UTF_8);
        MvcTestResult started = startVideo(sakura, video.length);
        assertThat(started).hasStatus(HttpStatus.CREATED);
        assertThat(json(started, "$.endpoint")).endsWith("/api/v1/dev/fake-video/tus");
        String mediaId = json(started, "$.mediaId");

        MvcTestResult list = send("GET", sakura.owner(), sakura.media(), null);
        assertThat(json(list, "$.content[0].status")).isEqualTo("PROCESSING");
        assertThat(json(list, "$.content[0].type")).isEqualTo("VIDEO");

        MvcTestResult created = tusCreate(started, video.length);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String location = created.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(tusPatch(location, 0, Arrays.copyOfRange(video, 0, 7))).hasStatus(HttpStatus.NO_CONTENT);
        // Pedaço fora de ordem: o servidor TUS recusa, e o navegador pergunta (HEAD) até onde chegou.
        assertThat(tusPatch(location, 3, new byte[]{1, 2})).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.head().uri(location).exchange().getResponse().getHeader("Upload-Offset")).isEqualTo("7");
        MvcTestResult last = tusPatch(location, 7, Arrays.copyOfRange(video, 7, video.length));
        assertThat(last.getResponse().getHeader("Upload-Offset")).isEqualTo(String.valueOf(video.length));

        MvcTestResult ready = send("GET", sakura.owner(), sakura.media(), null);
        assertThat(json(ready, "$.content[0].id")).isEqualTo(mediaId);
        assertThat(json(ready, "$.content[0].status")).isEqualTo("READY");
        assertThat(json(ready, "$.content[0].video.durationSeconds")).isEqualTo("15");
        assertThat(json(ready, "$.content[0].width")).isEqualTo("1080");
        String url = json(ready, "$.content[0].video.url");
        MvcTestResult played = mvc.get().uri(URI.create(url).getPath()).exchange();
        assertThat(played).hasStatusOk();
        assertThat(played.getResponse().getContentAsByteArray()).isEqualTo(video);

        assertThat(send("DELETE", sakura.owner(), sakura.media() + "/" + mediaId, null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(URI.create(url).getPath()).exchange()).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void theUploadNeedsItsSignatureAndTheLimitsHold() throws Exception {
        MvcTestResult started = startVideo(sakura, 10);
        Map<String, String> headers = JsonPath.read(started.getResponse().getContentAsString(), "$.headers");
        MvcTestResult forged = mvc.post().uri("/api/v1/dev/fake-video/tus").header("Upload-Length", "10")
                .header("VideoId", headers.get("VideoId"))
                .header("AuthorizationExpire", headers.get("AuthorizationExpire"))
                .header("AuthorizationSignature", "0".repeat(64)).exchange();
        assertThat(forged).hasStatus(HttpStatus.FORBIDDEN);

        assertThat(send("POST", sakura.owner(), sakura.videos(),
                "{\"contentType\":\"video/x-msvideo\",\"sizeBytes\":10}")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(startVideo(sakura, 200L * 1024 * 1024 + 1)).hasStatus(HttpStatus.BAD_REQUEST);
        startVideo(sakura, 10);
        startVideo(sakura, 10);
        assertThat(startVideo(sakura, 10)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void theWebhookNeedsTheTokenAndLooksTheStatusUpItself() throws Exception {
        MvcTestResult started = startVideo(sakura, 10);
        Map<String, String> headers = JsonPath.read(started.getResponse().getContentAsString(), "$.headers");
        String videoId = headers.get("VideoId");

        assertThat(webhook("token-errado", videoId)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(webhook("", videoId)).hasStatus(HttpStatus.UNAUTHORIZED);
        // O aviso diz "mudou", mas o serviço diz que o arquivo nem chegou: continua em conversão.
        assertThat(webhook(TestcontainersConfiguration.WEBHOOK_TOKEN, videoId)).hasStatusOk();
        assertThat(webhook(TestcontainersConfiguration.WEBHOOK_TOKEN, "video-que-nao-existe")).hasStatusOk();
        assertThat(json(send("GET", sakura.owner(), sakura.media(), null), "$.content[0].status"))
                .isEqualTo("PROCESSING");
    }

    @Test
    void onlyTheManagementAndOnlyOfTheirOwnProducts() throws Exception {
        String mediaId = json(startVideo(sakura, 10), "$.mediaId");
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();
        Restaurant other = newRestaurant("Outro Bar");

        assertThat(send("POST", waiter, sakura.videos(), "{\"contentType\":\"video/mp4\",\"sizeBytes\":10}"))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("POST", other.owner(), sakura.videos(), "{\"contentType\":\"video/mp4\",\"sizeBytes\":10}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.owner(), sakura.media(), null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.owner(), sakura.media() + "/" + mediaId, null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(json(send("GET", sakura.owner(), sakura.media(), null), "$.totalElements")).isEqualTo("1");
    }

    @Test
    void onlyTheReadyVideoGoesToTheTablesMenu() throws Exception {
        String sector = json(send("POST", sakura.owner(), "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr = json(send("POST", sakura.owner(), "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        byte[] video = "um vídeo de mentira".getBytes(StandardCharsets.UTF_8);
        MvcTestResult ready = startVideo(sakura, video.length);
        String location = tusCreate(ready, video.length).getResponse().getHeader(HttpHeaders.LOCATION);
        tusPatch(location, 0, video);
        // O cardápio da mesa lê só o banco: o vídeo fica "pronto" lá quando o serviço avisa.
        Map<String, String> headers = JsonPath.read(ready.getResponse().getContentAsString(), "$.headers");
        assertThat(webhook(TestcontainersConfiguration.WEBHOOK_TOKEN, headers.get("VideoId"))).hasStatusOk();
        startVideo(sakura, 10); // este fica em conversão: o arquivo nunca chega

        MvcTestResult menu = mvc.get().uri("/api/v1/public/tables/" + qr + "/menu").exchange();

        assertThat(menu).hasStatusOk();
        assertThat(json(menu, "$.categories[0].products[0].media.length()")).isEqualTo("1");
        assertThat(json(menu, "$.categories[0].products[0].media[0].type")).isEqualTo("VIDEO");
        assertThat(json(menu, "$.categories[0].products[0].media[0].video.durationSeconds")).isEqualTo("15");
        assertThat(json(menu, "$.categories[0].products[0].media[0].video.url")).endsWith("/play");
        // Sem foto, sem capa: a lista do cardápio não mostra vídeo no lugar da foto.
        assertThat(JsonPath.<Object>read(menu.getResponse().getContentAsString(), "$.categories[0].products[0].photo"))
                .isNull();
    }
}
