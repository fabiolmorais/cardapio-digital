package br.com.cardapio.adapter.in.web.presence;

import br.com.cardapio.domain.presence.model.GeoFence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** A configuração inteira da tela "Presença". Os valores (IP, raio) são conferidos de novo no domínio. */
@Schema(name = "PresenceRuleRequest")
record PresenceRuleRequest(
        @Schema(requiredMode = REQUIRED, description = "IPs ou faixas CIDR da internet do salão (até 10)",
                example = "[\"189.45.212.18\", \"2804:14c:6a::/64\"]")
        @NotNull @Size(max = 10) List<@NotBlank @Size(max = 50) String> networks,
        @Schema(description = "O nome do Wi-Fi do salão (sem a senha)", example = "Sakura Clientes")
        @Size(max = 60) String wifiName,
        @Schema(description = "O local e o raio; vazio para não usar a localização") @Valid Location location,
        @Schema(description = "Quanto tempo sem uso a sessão da mesa aguenta (30 a 360); vazio mantém como está",
                example = "120")
        @Min(30) @Max(360) Integer sessionIdleMinutes
) {

    @Schema(name = "PresenceLocation")
    record Location(
            @Schema(requiredMode = REQUIRED, example = "-23.5577") @NotNull Double latitude,
            @Schema(requiredMode = REQUIRED, example = "-46.6353") @NotNull Double longitude,
            @Schema(requiredMode = REQUIRED, description = "De 25 a 1000 metros", example = "100")
            @NotNull Integer radiusMeters
    ) {
        GeoFence toGeoFence() {
            return new GeoFence(latitude, longitude, radiusMeters);
        }
    }
}
