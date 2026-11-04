package br.com.cardapio.adapter.out.video;

import br.com.cardapio.application.port.out.video.HostedVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * O adaptador do Bunny Stream contra um servidor de mentira (o MockRestServiceServer do próprio
 * Spring): confere o que vai na requisição e como a resposta é lida, sem rede e sem conta.
 */
class BunnyVideoHostingTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private final VideoProperties.Bunny bunny = new VideoProperties.Bunny("4242", "chave-de-teste",
            "vz-abc.b-cdn.net", URI.create("https://api.bunny.test"));
    private MockRestServiceServer server;
    private BunnyVideoHosting hosting;

    @BeforeEach
    void bunny() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        hosting = new BunnyVideoHosting(bunny, builder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTheVideoWithTheApiKeyOnlyInTheHeader() {
        server.expect(requestTo("https://api.bunny.test/library/4242/videos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("AccessKey", "chave-de-teste"))
                .andExpect(content().json("{\"title\":\"Guioza\"}"))
                .andRespond(withSuccess("{\"guid\":\"abc-123\",\"status\":0}", MediaType.APPLICATION_JSON));

        assertThat(hosting.createVideo("Guioza")).isEqualTo("abc-123");
        server.verify();
    }

    @Test
    void theUploadSignatureIsTheOneTheBunnyAsksFor() {
        var ticket = hosting.signUpload("abc-123", Duration.ofHours(2));

        String expire = String.valueOf(NOW.plus(Duration.ofHours(2)).getEpochSecond());
        assertThat(ticket.endpoint()).isEqualTo("https://video.bunnycdn.com/tusupload");
        assertThat(ticket.headers())
                .containsEntry("AuthorizationSignature",
                        BunnyVideoHosting.sha256("4242chave-de-teste" + expire + "abc-123"))
                .containsEntry("AuthorizationExpire", expire)
                .containsEntry("VideoId", "abc-123")
                .containsEntry("LibraryId", "4242")
                .doesNotContainValue("chave-de-teste");
    }

    @Test
    void readsTheStatusAndTheMeasures() {
        server.expect(requestTo("https://api.bunny.test/library/4242/videos/abc-123"))
                .andRespond(withSuccess("{\"guid\":\"abc-123\",\"status\":4,\"width\":1080,\"height\":1920,"
                        + "\"length\":18}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.bunny.test/library/4242/videos/sumiu"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(hosting.find("abc-123")).contains(new HostedVideo(HostedVideo.Status.READY, 1080, 1920, 18));
        assertThat(hosting.find("sumiu")).isEmpty();
        server.verify();
    }

    @Test
    void mapsEachBunnyStatus() {
        for (int status : new int[]{0, 1, 2, 3}) {
            assertThat(BunnyVideoHosting.toHosted(new BunnyVideoHosting.BunnyVideo("x", status, null, null, null))
                    .status()).isEqualTo(HostedVideo.Status.PROCESSING);
        }
        assertThat(BunnyVideoHosting.toHosted(new BunnyVideoHosting.BunnyVideo("x", 5, null, null, null)).status())
                .isEqualTo(HostedVideo.Status.FAILED);
        assertThat(BunnyVideoHosting.toHosted(new BunnyVideoHosting.BunnyVideo("x", 6, null, null, null)).status())
                .isEqualTo(HostedVideo.Status.FAILED);
    }

    @Test
    void playsTheMp4FromTheCdn() {
        assertThat(hosting.playbackUrl("abc-123")).isEqualTo("https://vz-abc.b-cdn.net/abc-123/play_720p.mp4");
        assertThat(hosting.posterUrl("abc-123")).isEqualTo("https://vz-abc.b-cdn.net/abc-123/thumbnail.jpg");
    }
}
