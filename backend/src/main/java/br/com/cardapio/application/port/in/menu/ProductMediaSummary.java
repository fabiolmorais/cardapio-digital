package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.MediaType;
import br.com.cardapio.domain.menu.model.ProductMediaId;

import java.util.Map;

/**
 * A mídia para a tela. Foto: o endereço de cada tamanho. Vídeo: o status da conversão e, quando
 * pronto, o endereço para tocar, a capa e a duração.
 *
 * @param urls            um endereço por tamanho da foto; vazio no vídeo
 * @param videoUrl        o vídeo pronto, para tocar; null na foto e no vídeo que não está pronto
 * @param posterUrl       a capa do vídeo pronto; null se não há
 * @param durationSeconds a duração do vídeo pronto
 * @param failure         por que o vídeo não ficou pronto
 */
public record ProductMediaSummary(ProductMediaId id, MediaType type, int position, int width, int height,
                                  Map<ImageVariant, String> urls, MediaStatus status, String videoUrl,
                                  String posterUrl, Integer durationSeconds, String failure) {

    public ProductMediaSummary {
        urls = Map.copyOf(urls);
    }
}
