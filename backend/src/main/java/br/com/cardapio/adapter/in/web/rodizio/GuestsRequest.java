package br.com.cardapio.adapter.in.web.rodizio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Pessoas por faixa. A faixa que não vem fica com 0. */
@Schema(name = "RodizioGuestsRequest")
record GuestsRequest(
        @Schema(requiredMode = REQUIRED) @NotEmpty @Size(max = 6) List<@NotNull @Valid Guest> guests
) {

    @Schema(name = "RodizioGuestCount")
    record Guest(
            @Schema(requiredMode = REQUIRED, example = "Adulto") @NotBlank @Size(max = 40) String tierName,
            @Schema(requiredMode = REQUIRED, example = "4") @Min(0) @Max(99) int quantity
    ) {
    }

    /** Uma faixa repetida soma (o mesmo nome duas vezes não perde ninguém). */
    Map<String, Integer> toMap() {
        return guests.stream().collect(Collectors.toMap(Guest::tierName, Guest::quantity, Integer::sum));
    }
}
