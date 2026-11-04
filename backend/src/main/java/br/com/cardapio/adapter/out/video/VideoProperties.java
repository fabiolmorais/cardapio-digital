package br.com.cardapio.adapter.out.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;

/**
 * O serviço de vídeo (app.video.*). Até a Etapa 15, o falso (FAKE): o próprio back recebe o
 * envio e devolve o arquivo como se estivesse convertido. Com a conta do Bunny Stream, BUNNY.
 *
 * @param webhookToken o segredo que o serviço de vídeo manda no aviso (?token=); vazio recusa todo aviso
 */
@ConfigurationProperties(prefix = "app.video")
public record VideoProperties(Provider provider, String webhookToken, Bunny bunny, Fake fake) {

    public enum Provider {
        BUNNY,
        FAKE
    }

    /**
     * A biblioteca de vídeo do Bunny Stream.
     *
     * @param cdnHost o "CDN Hostname" da biblioteca (vz-….b-cdn.net), de onde o celular baixa
     * @param apiUrl  a API do Bunny Stream (muda só nos testes)
     */
    public record Bunny(String libraryId, String apiKey, String cdnHost, URI apiUrl) {

        public Bunny {
            apiUrl = apiUrl == null ? URI.create("https://video.bunnycdn.com") : apiUrl;
        }
    }

    /**
     * O falso.
     *
     * @param publicUrl  o endereço do back, como o navegador o vê (o envio e o vídeo vêm daqui)
     * @param storageDir onde guardar os arquivos recebidos (uma pasta temporária)
     */
    public record Fake(URI publicUrl, Path storageDir) {

        public Fake {
            publicUrl = publicUrl == null ? URI.create("http://localhost:8080") : publicUrl;
            storageDir = storageDir == null
                    ? Path.of(System.getProperty("java.io.tmpdir"), "cardapio-fake-video") : storageDir;
        }
    }

    public VideoProperties {
        provider = provider == null ? Provider.FAKE : provider;
        webhookToken = webhookToken == null ? "" : webhookToken;
        bunny = bunny == null ? new Bunny(null, null, null, null) : bunny;
        fake = fake == null ? new Fake(null, null) : fake;
        if (provider == Provider.BUNNY && (isBlank(bunny.libraryId()) || isBlank(bunny.apiKey())
                || isBlank(bunny.cdnHost()))) {
            throw new IllegalStateException(
                    "Configure o Bunny Stream: BUNNY_LIBRARY_ID, BUNNY_API_KEY e BUNNY_CDN_HOST (ver .env.example)");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
