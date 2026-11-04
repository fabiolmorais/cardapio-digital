package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.ProductMediaId;

import java.time.Instant;
import java.util.Map;

/**
 * O envio do vídeo combinado: o navegador envia por TUS para o endpoint, com estes cabeçalhos,
 * até expiresAt. A mídia já existe, em conversão (mediaId). Os campos são copiados da porta de
 * saída, como no MediaUpload da foto: o controller não conhece as portas de saída.
 */
public record VideoUpload(ProductMediaId mediaId, String endpoint, Map<String, String> headers, Instant expiresAt) {

    public VideoUpload {
        headers = Map.copyOf(headers);
    }
}
