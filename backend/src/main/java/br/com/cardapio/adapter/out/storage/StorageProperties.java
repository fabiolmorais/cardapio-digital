package br.com.cardapio.adapter.out.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Onde ficam as fotos (app.storage.*). No dia a dia, um bucket do Cloudflare R2; nos testes,
 * o S3Mock. As chaves vêm do .env (STORAGE_ACCESS_KEY_ID e STORAGE_SECRET_ACCESS_KEY).
 *
 * @param endpoint  a API S3 do serviço (no R2: https://{account-id}.r2.cloudflarestorage.com)
 * @param publicUrl de onde o navegador baixa as fotos (no R2 em desenvolvimento: a URL r2.dev do bucket)
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        URI endpoint,
        String region,
        String bucket,
        String accessKeyId,
        String secretAccessKey,
        URI publicUrl
) {

    public StorageProperties {
        if (endpoint == null || publicUrl == null || isBlank(bucket) || isBlank(accessKeyId)
                || isBlank(secretAccessKey)) {
            throw new IllegalStateException(
                    "Configure o armazenamento das fotos: STORAGE_ENDPOINT, STORAGE_BUCKET, STORAGE_PUBLIC_URL, "
                            + "STORAGE_ACCESS_KEY_ID e STORAGE_SECRET_ACCESS_KEY (ver .env.example)");
        }
        region = isBlank(region) ? "auto" : region; // o R2 usa "auto"
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
