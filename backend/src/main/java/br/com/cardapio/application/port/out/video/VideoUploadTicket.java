package br.com.cardapio.application.port.out.video;

import java.time.Instant;
import java.util.Map;

/**
 * Como o navegador envia o vídeo: pelo protocolo TUS (envio em partes, que retoma se a rede
 * cair), neste endereço, com estes cabeçalhos, até expiresAt.
 */
public record VideoUploadTicket(String endpoint, Map<String, String> headers, Instant expiresAt) {

    public VideoUploadTicket {
        headers = Map.copyOf(headers);
    }
}
