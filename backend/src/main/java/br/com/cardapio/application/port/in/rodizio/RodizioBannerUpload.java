package br.com.cardapio.application.port.in.rodizio;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** O envio do banner combinado, igual ao da logo: PUT na url com estes cabeçalhos, depois avisar com o uploadId. */
public record RodizioBannerUpload(UUID uploadId, String url, Map<String, String> headers, Instant expiresAt) {

    public RodizioBannerUpload {
        headers = Map.copyOf(headers);
    }
}
