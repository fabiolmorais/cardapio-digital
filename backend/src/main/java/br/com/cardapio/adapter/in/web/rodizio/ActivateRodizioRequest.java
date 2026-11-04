package br.com.cardapio.adapter.in.web.rodizio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ActivateRodizioRequest")
record ActivateRodizioRequest(
        @Schema(requiredMode = REQUIRED) @NotNull UUID rodizioId,
        @Schema(requiredMode = REQUIRED) @NotEmpty @Size(max = 6) List<GuestsRequest.@NotNull @Valid Guest> guests
) {

    Map<String, Integer> guestMap() {
        return new GuestsRequest(guests).toMap();
    }
}
