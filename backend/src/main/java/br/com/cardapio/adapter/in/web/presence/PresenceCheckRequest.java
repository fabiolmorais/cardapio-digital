package br.com.cardapio.adapter.in.web.presence;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A localização do celular, quando o cliente toca em "Usar minha localização". Sem corpo (ou com
 * os campos vazios), a conferência é só pela rede.
 */
@Schema(name = "PresenceCheckRequest")
record PresenceCheckRequest(
        @Schema(example = "-23.5577") Double latitude,
        @Schema(example = "-46.6353") Double longitude,
        @Schema(description = "Precisão informada pelo navegador, em metros", example = "25") Double accuracyMeters
) {

    boolean hasPosition() {
        return latitude != null && longitude != null && accuracyMeters != null;
    }

    /** A localização é dado pessoal: nunca vai para o log. */
    @Override
    public String toString() {
        return "PresenceCheckRequest[hasPosition=" + hasPosition() + "]";
    }
}
