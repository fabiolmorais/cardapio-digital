package br.com.cardapio.adapter.in.web.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "SetLogoRequest")
record SetLogoRequest(@Schema(requiredMode = REQUIRED) @NotNull UUID uploadId) {
}
