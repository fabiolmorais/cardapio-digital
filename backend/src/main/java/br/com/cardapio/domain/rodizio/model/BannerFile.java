package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Set;

/**
 * O banner do rodízio ("Escolha o rodízio", na mesa), como o navegador o descreve antes de enviar.
 * O arquivo de verdade é conferido de novo depois de chegar (o tipo, pelos bytes, e as medidas).
 * O banner é largo: o ideal é 1200 × 600 (2:1); ele é cortado pelo centro nessa proporção.
 */
public record BannerFile(String contentType, long sizeBytes) {

    public static final Set<String> TYPES = Set.of("image/jpeg", "image/png");
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    /** O tamanho gravado (e o ideal para enviar). */
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 600;
    /** Menos que isso fica borrado na tela do celular. */
    public static final int MIN_WIDTH = 600;
    public static final int MIN_HEIGHT = 300;

    public BannerFile {
        if (contentType == null || !TYPES.contains(contentType)) {
            throw new InvalidValueException("Envie o banner em JPG ou PNG");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_BYTES) {
            throw new InvalidValueException("O banner precisa ter até 5 MB");
        }
    }
}
