package br.com.cardapio.adapter.out.storage;

import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.media.SignedUpload;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * O armazenamento pela API S3, que o Cloudflare R2 implementa. Trocar de serviço (R2, S3 da
 * AWS, S3Mock) é só trocar o endpoint e as chaves.
 */
@Component
class S3MediaStorage implements MediaStoragePort {

    /** Os arquivos gerados nunca mudam (uma foto nova ganha outra pasta): o navegador guarda por um ano. */
    private static final String CACHE_FOREVER = "public, max-age=31536000, immutable";

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    S3MediaStorage(S3Client s3, S3Presigner presigner, StorageProperties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.properties = properties;
    }

    /**
     * O tipo e o tamanho entram na assinatura: o R2 recusa o PUT se o navegador mandar outro
     * tipo ou outro tamanho. Quem pegar a URL não consegue usá-la para enviar outra coisa.
     */
    @Override
    public SignedUpload signUpload(String path, String contentType, long sizeBytes, Duration validity) {
        PresignedPutObjectRequest signed = presigner.presignPutObject(request -> request
                .signatureDuration(validity)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(path)
                        .contentType(contentType)
                        .contentLength(sizeBytes)
                        .build()));
        return new SignedUpload(signed.url().toString(), Map.of("Content-Type", contentType), signed.expiration());
    }

    @Override
    public Optional<byte[]> read(String path, long maxBytes) {
        try {
            long size = s3.headObject(h -> h.bucket(properties.bucket()).key(path)).contentLength();
            if (size > maxBytes) {
                return Optional.empty(); // maior que o combinado: tratado como envio que não valeu
            }
            return Optional.of(s3.getObjectAsBytes(g -> g.bucket(properties.bucket()).key(path)).asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            // O HEAD de um arquivo que não existe responde 404 sem corpo, e o SDK não sabe que é "NoSuchKey".
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void store(String path, byte[] content, String contentType) {
        s3.putObject(p -> p.bucket(properties.bucket()).key(path).contentType(contentType).cacheControl(CACHE_FOREVER),
                RequestBody.fromBytes(content));
    }

    /**
     * Os arquivos vão ao mesmo tempo, cada um numa thread virtual: o tempo total fica perto do
     * de um envio só, e não da soma. O S3Client pode ser usado por várias threads de uma vez
     * (ele tem um conjunto de conexões, o pool).
     */
    @Override
    public void storeAll(Map<String, byte[]> files, String contentType) {
        List<Future<?>> uploads;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            uploads = files.entrySet().stream()
                    .<Future<?>>map(file -> executor.submit(() -> store(file.getKey(), file.getValue(), contentType)))
                    .toList();
        } // o close() do executor espera todos os envios terminarem
        for (Future<?> upload : uploads) {
            try {
                upload.get();
            } catch (ExecutionException e) {
                throw e.getCause() instanceof RuntimeException cause ? cause : new IllegalStateException(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void delete(String path) {
        s3.deleteObject(d -> d.bucket(properties.bucket()).key(path));
    }

    @Override
    public String publicUrl(String path) {
        String base = properties.publicUrl().toString();
        return (base.endsWith("/") ? base : base + "/") + path;
    }
}
