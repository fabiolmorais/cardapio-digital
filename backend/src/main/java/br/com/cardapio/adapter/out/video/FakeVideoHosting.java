package br.com.cardapio.adapter.out.video;

import br.com.cardapio.application.port.out.video.HostedVideo;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.application.port.out.video.VideoUploadTicket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Um serviço de vídeo de mentira, para o desenvolvimento (até a conta do Bunny, na Etapa 15) e
 * para os testes. Fala o mesmo TUS do Bunny (o FakeVideoController), guarda o arquivo numa pasta
 * e, quando ele chega inteiro, diz que está "pronto": 1080 × 1920, 15 segundos. Não converte
 * nada: o vídeo toca como foi enviado. Liga com app.video.provider=fake.
 *
 * <p>O vídeo inteiro ganha, ao lado, um arquivo com o tipo dele: assim continua tocando depois
 * que o back reinicia, quando a lista em memória começa vazia.
 */
@Component
@ConditionalOnProperty(prefix = "app.video", name = "provider", havingValue = "fake")
class FakeVideoHosting implements VideoHostingPort {

    /** As medidas que o falso diz ter "convertido". */
    static final int WIDTH = 1080;
    static final int HEIGHT = 1920;
    static final int SECONDS = 15;

    /** Um vídeo do falso: quanto ele espera receber, quanto já chegou e de que tipo é. */
    record Upload(long length, long received, String contentType) {

        boolean complete() {
            return length > 0 && received == length;
        }
    }

    private final Map<String, Upload> videos = new ConcurrentHashMap<>();
    /** O segredo das assinaturas: muda a cada vez que o back sobe (um envio pela metade não é retomado). */
    private final String secret = HexFormat.of().formatHex(new SecureRandom().generateSeed(16));
    private final VideoProperties.Fake fake;
    private final Clock clock;

    FakeVideoHosting(VideoProperties properties, Clock clock) {
        this.fake = properties.fake();
        this.clock = clock;
    }

    @Override
    public String createVideo(String title) {
        String id = UUID.randomUUID().toString();
        videos.put(id, new Upload(0, 0, "video/mp4"));
        return id;
    }

    @Override
    public VideoUploadTicket signUpload(String videoId, Duration validity) {
        Instant expiresAt = clock.instant().plus(validity);
        String expire = String.valueOf(expiresAt.getEpochSecond());
        return new VideoUploadTicket(fake.publicUrl() + FakeVideoController.TUS_PATH, Map.of(
                "AuthorizationSignature", signature(videoId, expire),
                "AuthorizationExpire", expire,
                "VideoId", videoId,
                "LibraryId", "fake"), expiresAt);
    }

    @Override
    public Optional<HostedVideo> find(String videoId) {
        return upload(videoId).map(v -> v.complete()
                ? new HostedVideo(HostedVideo.Status.READY, WIDTH, HEIGHT, SECONDS)
                : new HostedVideo(HostedVideo.Status.PROCESSING, 0, 0, 0));
    }

    @Override
    public void deleteVideo(String videoId) {
        videos.remove(videoId);
        try {
            Files.deleteIfExists(file(videoId));
            Files.deleteIfExists(typeFile(videoId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String playbackUrl(String videoId) {
        return fake.publicUrl() + FakeVideoController.PATH + "/" + videoId + "/play";
    }

    /** O falso não gera capa: o celular mostra o primeiro quadro do vídeo. */
    @Override
    public String posterUrl(String videoId) {
        return null;
    }

    // --- O lado do "servidor TUS", que o FakeVideoController chama.

    /** O envio começou (TUS: POST). false se a assinatura não vale para este vídeo, ou venceu. */
    boolean begin(String videoId, String expire, String signature, long length, String contentType) {
        if (!videos.containsKey(videoId) || !signature(videoId, expire).equals(signature)
                || clock.instant().getEpochSecond() > Long.parseLong(expire)) {
            return false;
        }
        videos.put(videoId, new Upload(length, 0, contentType == null ? "video/mp4" : contentType));
        try {
            Files.createDirectories(fake.storageDir());
            Files.deleteIfExists(file(videoId));
            Files.deleteIfExists(typeFile(videoId));
            Files.createFile(file(videoId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    /** O envio deste vídeo: o da memória, ou, depois de um reinício, o vídeo inteiro que ficou na pasta. */
    Optional<Upload> upload(String videoId) {
        Upload known = videos.get(videoId);
        if (known != null) {
            return Optional.of(known);
        }
        return stored(videoId).map(upload -> {
            videos.putIfAbsent(videoId, upload);
            return upload;
        });
    }

    private Optional<Upload> stored(String videoId) {
        try {
            Path type = typeFile(videoId);
            Path file = file(videoId);
            if (!Files.isRegularFile(type) || !Files.isRegularFile(file)) {
                return Optional.empty();
            }
            long size = Files.size(file);
            return Optional.of(new Upload(size, size, Files.readString(type).strip()));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // não é um UUID: não é um vídeo do falso
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Mais um pedaço (TUS: PATCH), a partir de offset. Devolve o novo offset, ou -1 se fora de ordem. */
    long append(String videoId, long offset, InputStream body) {
        Upload current = videos.get(videoId);
        if (current == null || current.received() != offset) {
            return -1;
        }
        try (var out = Files.newOutputStream(file(videoId), StandardOpenOption.APPEND)) {
            long written = body.transferTo(out);
            long received = Math.min(current.length(), offset + written);
            Upload upload = new Upload(current.length(), received, current.contentType());
            if (upload.complete()) {
                Files.writeString(typeFile(videoId), upload.contentType());
            }
            videos.put(videoId, upload);
            return received;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path file(String videoId) {
        // O id é um UUID que o próprio falso gerou: nada de "../" chega ao caminho.
        return fake.storageDir().resolve(UUID.fromString(videoId) + ".video");
    }

    /** O tipo do vídeo inteiro: só existe depois que o último pedaço chegou. */
    private Path typeFile(String videoId) {
        return fake.storageDir().resolve(UUID.fromString(videoId) + ".type");
    }

    private String signature(String videoId, String expire) {
        return BunnyVideoHosting.sha256(secret + expire + videoId);
    }
}
