package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.RenderedFile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Resposta de arquivo para baixar: tipo certo, nome do arquivo e sem guardar em cache. */
final class Downloads {

    private Downloads() {
    }

    static ResponseEntity<byte[]> of(RenderedFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // O QR leva o token da mesa: nem o navegador nem um proxy devem guardar uma cópia.
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }
}
