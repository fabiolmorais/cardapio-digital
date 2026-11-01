package br.com.cardapio.application.port.out.media;

import java.time.Instant;
import java.util.Map;

/**
 * Como o navegador envia o arquivo: um PUT na url, com estes cabeçalhos, até expiresAt.
 */
public record SignedUpload(String url, Map<String, String> headers, Instant expiresAt) {

    public SignedUpload {
        headers = Map.copyOf(headers);
    }
}
