package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Onde o celular diz que está, com a precisão que o próprio navegador informa (em metros): "estou
 * a até 30 m deste ponto". Vem do cliente, então é um indício, não uma prova; por isso a precisão
 * ruim é recusada e a liberação pelo garçom continua existindo.
 */
public record GeoPosition(double latitude, double longitude, double accuracyMeters) {

    public GeoPosition {
        GeoFence.checkCoordinates(latitude, longitude);
        if (!(accuracyMeters >= 0)) {
            throw new InvalidValueException("Precisão da localização inválida");
        }
    }
}
