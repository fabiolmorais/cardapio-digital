package br.com.cardapio.application.port.in.menu;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * O envio combinado: o navegador faz um PUT na url, com estes cabeçalhos, até expiresAt, e
 * depois avisa com o uploadId. (Os campos são copiados da porta de saída: quem chama o caso de
 * uso, o controller, não pode conhecer as portas de saída. O ArchUnit confere.)
 */
public record MediaUpload(UUID uploadId, String url, Map<String, String> headers, Instant expiresAt) {

    public MediaUpload {
        headers = Map.copyOf(headers);
    }
}
