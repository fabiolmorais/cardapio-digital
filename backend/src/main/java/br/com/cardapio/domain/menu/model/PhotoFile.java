package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Set;

/**
 * O que o navegador diz sobre a foto antes de enviar: o tipo e o tamanho. É só a primeira
 * conferência: o que a pessoa diz pode ser mentira, e o arquivo de verdade é conferido de
 * novo depois de chegar (o tipo real, pelos bytes, e o tamanho).
 */
public record PhotoFile(String contentType, long sizeBytes) {

    public static final Set<String> TYPES = Set.of("image/jpeg", "image/png");
    public static final long MAX_BYTES = 8L * 1024 * 1024;

    public PhotoFile {
        if (contentType == null || !TYPES.contains(contentType)) {
            throw new InvalidValueException("Envie uma foto JPG ou PNG");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_BYTES) {
            throw new InvalidValueException("A foto precisa ter até 8 MB");
        }
    }
}
