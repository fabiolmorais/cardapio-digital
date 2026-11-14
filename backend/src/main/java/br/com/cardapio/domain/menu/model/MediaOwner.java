package br.com.cardapio.domain.menu.model;

import java.util.UUID;

/**
 * De quem é uma galeria de fotos e vídeos: de um produto ou de uma opção (o sabor da pizza com a
 * foto e o vídeo dele; complemento da ADR 0010). Os limites e o caminho do envio são os mesmos.
 */
public sealed interface MediaOwner permits ProductId, OptionItemId {

    UUID value();

    /** A primeira pasta no armazenamento: "products" ou "options". */
    String folder();
}
