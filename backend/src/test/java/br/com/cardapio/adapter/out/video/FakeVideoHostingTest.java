package br.com.cardapio.adapter.out.video;

import br.com.cardapio.application.port.out.video.HostedVideo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O serviço de vídeo falso guarda o que recebeu: depois que o back reinicia (um falso novo, com a
 * lista em memória vazia), o vídeo enviado inteiro continua pronto para tocar.
 */
class FakeVideoHostingTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
    private static final byte[] VIDEO = {1, 2, 3, 4, 5, 6, 7, 8};

    @TempDir
    Path dir;

    @Test
    void theWholeVideoStillPlaysAfterTheBackRestarts() throws Exception {
        FakeVideoHosting before = hosting();
        String id = sent(before, VIDEO.length);

        FakeVideoHosting after = hosting();

        assertThat(after.find(id)).contains(new HostedVideo(HostedVideo.Status.READY, 1080, 1920, 15));
        assertThat(after.upload(id)).hasValueSatisfying(upload -> {
            assertThat(upload.complete()).isTrue();
            assertThat(upload.contentType()).isEqualTo("video/webm");
        });
        assertThat(Files.readAllBytes(after.file(id))).isEqualTo(VIDEO);
    }

    @Test
    void anUploadCutInHalfIsNotTakenAsReady() {
        FakeVideoHosting before = hosting();
        String id = sent(before, VIDEO.length * 2);
        assertThat(before.find(id)).hasValueSatisfying(v -> assertThat(v.status()).isEqualTo(HostedVideo.Status.PROCESSING));

        assertThat(hosting().find(id)).isEmpty();
    }

    @Test
    void theDeletedVideoDoesNotComeBack() {
        FakeVideoHosting before = hosting();
        String id = sent(before, VIDEO.length);

        before.deleteVideo(id);

        assertThat(hosting().find(id)).isEmpty();
        assertThat(dir).isEmptyDirectory();
    }

    @Test
    void anIdThatIsNotOneOfItsVideosIsUnknown() {
        assertThat(hosting().find(UUID.randomUUID().toString())).isEmpty();
        assertThat(hosting().find("../segredo")).isEmpty();
    }

    private FakeVideoHosting hosting() {
        var properties = new VideoProperties(VideoProperties.Provider.FAKE, "", null,
                new VideoProperties.Fake(null, dir));
        return new FakeVideoHosting(properties, CLOCK);
    }

    /** Cria o vídeo e manda os bytes de VIDEO, de um envio de `length` bytes. */
    private static String sent(FakeVideoHosting hosting, long length) {
        String id = hosting.createVideo("Temaki");
        var headers = hosting.signUpload(id, Duration.ofMinutes(10)).headers();
        assertThat(hosting.begin(id, headers.get("AuthorizationExpire"), headers.get("AuthorizationSignature"),
                length, "video/webm")).isTrue();
        assertThat(hosting.append(id, 0, new ByteArrayInputStream(VIDEO))).isEqualTo(VIDEO.length);
        return id;
    }
}
