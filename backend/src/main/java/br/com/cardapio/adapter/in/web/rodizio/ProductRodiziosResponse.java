package br.com.cardapio.adapter.in.web.rodizio;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ProductRodizios", description = "Os rodízios em que o produto ficou")
record ProductRodiziosResponse(@Schema(requiredMode = REQUIRED) List<UUID> rodizioIds) {
}
