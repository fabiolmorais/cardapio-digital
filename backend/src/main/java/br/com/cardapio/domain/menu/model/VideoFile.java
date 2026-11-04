package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Set;

/**
 * O que o navegador diz sobre o vídeo antes de enviar: o tipo e o tamanho. Como na foto, é só a
 * primeira conferência: a duração de verdade chega do serviço de vídeo, depois da conversão.
 */
public record VideoFile(String contentType, long sizeBytes) {

    /** MP4 (Android e a maioria das câmeras), MOV (iPhone) e WebM. */
    public static final Set<String> TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");
    public static final long MAX_BYTES = 200L * 1024 * 1024;

    public VideoFile {
        if (contentType == null || !TYPES.contains(contentType)) {
            throw new InvalidValueException("Envie um vídeo MP4, MOV ou WebM");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_BYTES) {
            throw new InvalidValueException("O vídeo precisa ter até 200 MB");
        }
    }
}
