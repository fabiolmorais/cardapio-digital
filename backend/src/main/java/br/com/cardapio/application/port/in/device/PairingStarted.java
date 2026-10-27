package br.com.cardapio.application.port.in.device;

import br.com.cardapio.domain.device.model.PairingCode;
import br.com.cardapio.domain.device.model.PairingId;

import java.time.Instant;

/**
 * Resposta para o aparelho novo: o código vai para a tela; o segredo fica guardado com ele,
 * para provar depois que é ele mesmo quem busca o token.
 */
public record PairingStarted(PairingId id, PairingCode code, String secret, Instant expiresAt) {

    @Override
    public String toString() {
        return "PairingStarted[id=" + id + ", expiresAt=" + expiresAt + "]";
    }
}
