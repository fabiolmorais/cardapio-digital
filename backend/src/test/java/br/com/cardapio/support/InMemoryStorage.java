package br.com.cardapio.support;

import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.media.SignedUpload;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** O armazenamento (R2) em memória, para os testes dos casos de uso: caminho → bytes. */
public final class InMemoryStorage implements MediaStoragePort {

    public final Map<String, byte[]> files = new LinkedHashMap<>();

    /** Faz o papel do navegador: envia o arquivo para a URL assinada. */
    public void browserUploads(String url, byte[] content) {
        files.put(url.replace("https://r2.test/", ""), content);
    }

    @Override
    public SignedUpload signUpload(String path, String contentType, long sizeBytes, Duration validity) {
        return new SignedUpload("https://r2.test/" + path, Map.of("Content-Type", contentType),
                Instant.parse("2026-11-02T12:05:00Z"));
    }

    @Override
    public Optional<byte[]> read(String path, long maxBytes) {
        return Optional.ofNullable(files.get(path)).filter(f -> f.length <= maxBytes);
    }

    @Override
    public void store(String path, byte[] content, String contentType) {
        files.put(path, content);
    }

    @Override
    public void delete(String path) {
        files.remove(path);
    }

    @Override
    public String publicUrl(String path) {
        return "https://fotos.test/" + path;
    }
}
