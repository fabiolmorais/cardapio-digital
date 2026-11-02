package br.com.cardapio.application.port.in.brand;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** O envio da logo combinado, igual ao das fotos: PUT na url com estes cabeçalhos, depois avisar com o uploadId. */
public record LogoUpload(UUID uploadId, String url, Map<String, String> headers, Instant expiresAt) {

    public LogoUpload {
        headers = Map.copyOf(headers);
    }
}
