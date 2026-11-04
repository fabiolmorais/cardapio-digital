package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.SyncVideoUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * O aviso do serviço de vídeo (webhook): "este vídeo mudou de status". Não tem login: quem chama
 * é o Bunny. Vale só com o token secreto na URL (cadastrada no painel do Bunny), e o aviso só diz
 * QUAL vídeo: o status é consultado de novo no próprio serviço, então um aviso falso não faz um
 * vídeo aparecer pronto. Fora da documentação da API (não é para o front).
 */
@RestController
@Hidden
class VideoWebhookController {

    private final SyncVideoUseCase syncVideo;
    private final byte[] token;

    VideoWebhookController(SyncVideoUseCase syncVideo, @Value("${app.video.webhook-token:}") String token) {
        this.syncVideo = syncVideo;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    /** O corpo do aviso do Bunny Stream (os nomes são os dele). */
    record BunnyEvent(@JsonProperty("VideoGuid") String videoGuid, @JsonProperty("Status") Integer status) {
    }

    @PostMapping("/api/v1/webhooks/video")
    ResponseEntity<Void> receive(@RequestParam(defaultValue = "") String token, @RequestBody BunnyEvent event) {
        // Comparação em tempo constante: quem tenta adivinhar o token não aprende nada pelo tempo.
        if (this.token.length == 0
                || !MessageDigest.isEqual(this.token, token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (event.videoGuid() != null && !event.videoGuid().isBlank()) {
            syncVideo.sync(event.videoGuid());
        }
        // Vídeo desconhecido também é 200: o Bunny para de repetir o aviso.
        return ResponseEntity.ok().build();
    }
}
