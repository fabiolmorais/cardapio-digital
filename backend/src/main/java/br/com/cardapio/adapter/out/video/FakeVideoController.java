package br.com.cardapio.adapter.out.video;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * O "servidor TUS" do serviço de vídeo falso: o mínimo do protocolo que o navegador usa para
 * enviar (criar o envio, mandar os pedaços, perguntar até onde foi) e o endereço para tocar.
 * Só existe com o falso ligado; fora da documentação da API.
 */
@RestController
@Hidden
@ConditionalOnProperty(prefix = "app.video", name = "provider", havingValue = "fake")
@RequestMapping(FakeVideoController.PATH)
class FakeVideoController {

    static final String PATH = "/api/v1/dev/fake-video";
    static final String TUS_PATH = PATH + "/tus";
    private static final String TUS_VERSION = "1.0.0";

    private final FakeVideoHosting videos;

    FakeVideoController(FakeVideoHosting videos) {
        this.videos = videos;
    }

    /** TUS: cria o envio. Os cabeçalhos são os que o signUpload deu ao navegador. */
    @PostMapping("/tus")
    ResponseEntity<Void> create(@RequestHeader("VideoId") String videoId,
                                @RequestHeader("AuthorizationExpire") String expire,
                                @RequestHeader("AuthorizationSignature") String signature,
                                @RequestHeader("Upload-Length") long length,
                                @RequestHeader(value = "Upload-Metadata", required = false) String metadata) {
        if (!videos.begin(videoId, expire, signature, length, fileType(metadata))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).header("Tus-Resumable", TUS_VERSION).build();
        }
        return ResponseEntity.created(URI.create(TUS_PATH + "/" + videoId))
                .header("Tus-Resumable", TUS_VERSION).build();
    }

    /** TUS: até onde chegou (para retomar depois de uma queda). */
    @RequestMapping(value = "/tus/{videoId}", method = RequestMethod.HEAD)
    ResponseEntity<Void> offset(@PathVariable String videoId) {
        return videos.upload(videoId)
                .map(u -> ResponseEntity.ok().header("Tus-Resumable", TUS_VERSION)
                        .header("Upload-Offset", String.valueOf(u.received()))
                        .header("Upload-Length", String.valueOf(u.length()))
                        .header(HttpHeaders.CACHE_CONTROL, "no-store").<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    /** TUS: mais um pedaço do arquivo. */
    @PatchMapping(value = "/tus/{videoId}", consumes = "application/offset+octet-stream")
    ResponseEntity<Void> append(@PathVariable String videoId, @RequestHeader("Upload-Offset") long offset,
                                InputStream body) {
        if (videos.upload(videoId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        long received = videos.append(videoId, offset, body);
        if (received < 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header("Tus-Resumable", TUS_VERSION).build();
        }
        return ResponseEntity.noContent().header("Tus-Resumable", TUS_VERSION)
                .header("Upload-Offset", String.valueOf(received)).build();
    }

    /** O vídeo "convertido": o próprio arquivo enviado. O Spring atende o Range que o player pede. */
    @GetMapping("/{videoId}/play")
    ResponseEntity<Resource> play(@PathVariable String videoId) throws IOException {
        var upload = videos.upload(videoId).filter(FakeVideoHosting.Upload::complete);
        if (upload.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(upload.get().contentType()))
                .body(new FileSystemResource(videos.file(videoId)));
    }

    /** O tipo do arquivo, que o TUS manda em Upload-Metadata ("filetype <base64>, …"). */
    static String fileType(String metadata) {
        if (metadata == null) {
            return null;
        }
        for (String pair : metadata.split(",")) {
            String[] kv = pair.trim().split(" ", 2);
            if (kv.length == 2 && kv[0].equals("filetype")) {
                String type = new String(Base64.getDecoder().decode(kv[1]), StandardCharsets.UTF_8);
                return type.startsWith("video/") ? type : null;
            }
        }
        return null;
    }
}
