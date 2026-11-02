package br.com.cardapio.adapter.out.storage;

import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O armazenamento de verdade (o S3Mock fazendo o papel do R2). */
@IntegrationTest
class S3MediaStorageTest {

    @Autowired
    MediaStoragePort storage;

    @Test
    void storeAllSavesEveryFileEvenSendingThemTogether() {
        String folder = "teste/" + UUID.randomUUID();
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (String size : new String[]{"thumb", "medium", "large", "extra-1", "extra-2"}) {
            files.put(folder + "/" + size + ".jpg", ("conteúdo " + size).getBytes(StandardCharsets.UTF_8));
        }

        storage.storeAll(files, "image/jpeg");

        files.forEach((path, content) -> assertThat(storage.read(path, 1024)).as(path).hasValue(content));
    }
}
