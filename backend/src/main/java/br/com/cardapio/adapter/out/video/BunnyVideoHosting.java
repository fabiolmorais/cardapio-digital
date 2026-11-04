package br.com.cardapio.adapter.out.video;

import br.com.cardapio.application.port.out.video.HostedVideo;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.application.port.out.video.VideoUploadTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * O Bunny Stream de verdade, pela API REST dele (o RestClient do próprio Spring, sem biblioteca
 * nova). A chave da API vai só no cabeçalho AccessKey, daqui para o Bunny; para o navegador vai
 * uma assinatura do envio de um vídeo só, que vence. Ligado com app.video.provider=bunny.
 */
@Component
@ConditionalOnProperty(prefix = "app.video", name = "provider", havingValue = "bunny")
class BunnyVideoHosting implements VideoHostingPort {

    /** O envio direto do navegador, pelo protocolo TUS. */
    static final String TUS_ENDPOINT = "https://video.bunnycdn.com/tusupload";

    private final VideoProperties.Bunny bunny;
    private final RestClient api;
    private final Clock clock;

    @Autowired
    BunnyVideoHosting(VideoProperties properties, Clock clock) {
        this(properties.bunny(), RestClient.builder(), clock);
    }

    /** Os testes passam um builder ligado a um servidor de mentira (MockRestServiceServer). */
    BunnyVideoHosting(VideoProperties.Bunny bunny, RestClient.Builder builder, Clock clock) {
        this.bunny = bunny;
        this.api = builder.baseUrl(bunny.apiUrl().toString())
                .defaultHeader("AccessKey", bunny.apiKey())
                .build();
        this.clock = clock;
    }

    /** O pedaço da resposta do Bunny que o back usa. */
    record BunnyVideo(String guid, Integer status, Integer width, Integer height, Integer length) {
    }

    @Override
    public String createVideo(String title) {
        BunnyVideo created = api.post().uri("/library/{library}/videos", bunny.libraryId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", title))
                .retrieve()
                .body(BunnyVideo.class);
        if (created == null || created.guid() == null) {
            throw new IllegalStateException("O Bunny não devolveu o id do vídeo");
        }
        return created.guid();
    }

    /**
     * A assinatura que o Bunny pede no envio TUS: SHA-256 de (biblioteca + chave + validade +
     * vídeo), em hexadecimal. Quem tem a assinatura envia este vídeo, e só até a validade.
     */
    @Override
    public VideoUploadTicket signUpload(String videoId, Duration validity) {
        Instant expiresAt = clock.instant().plus(validity);
        String expire = String.valueOf(expiresAt.getEpochSecond());
        return new VideoUploadTicket(TUS_ENDPOINT, Map.of(
                "AuthorizationSignature", sha256(bunny.libraryId() + bunny.apiKey() + expire + videoId),
                "AuthorizationExpire", expire,
                "VideoId", videoId,
                "LibraryId", bunny.libraryId()), expiresAt);
    }

    @Override
    public Optional<HostedVideo> find(String videoId) {
        try {
            BunnyVideo video = api.get().uri("/library/{library}/videos/{id}", bunny.libraryId(), videoId)
                    .retrieve()
                    .body(BunnyVideo.class);
            return Optional.ofNullable(video).map(BunnyVideoHosting::toHosted);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Os status do Bunny: 0 criado, 1 enviado, 2 processando, 3 convertendo, 4 pronto, 5 erro,
     * 6 envio falhou. Do 0 ao 3 ainda não dá para tocar.
     */
    static HostedVideo toHosted(BunnyVideo v) {
        int status = v.status() == null ? 0 : v.status();
        HostedVideo.Status mapped = switch (status) {
            case 4 -> HostedVideo.Status.READY;
            case 5, 6 -> HostedVideo.Status.FAILED;
            default -> HostedVideo.Status.PROCESSING;
        };
        return new HostedVideo(mapped, orZero(v.width()), orZero(v.height()), orZero(v.length()));
    }

    @Override
    public void deleteVideo(String videoId) {
        try {
            api.delete().uri("/library/{library}/videos/{id}", bunny.libraryId(), videoId)
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // Já não estava lá: o resultado é o mesmo.
        }
    }

    /** O MP4 de 720p (a "MP4 Fallback" ligada na biblioteca): o celular toca sem biblioteca de HLS. */
    @Override
    public String playbackUrl(String videoId) {
        return "https://" + bunny.cdnHost() + "/" + videoId + "/play_720p.mp4";
    }

    @Override
    public String posterUrl(String videoId) {
        return "https://" + bunny.cdnHost() + "/" + videoId + "/thumbnail.jpg";
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
