package br.com.cardapio.application.port.out.device;

import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.PairingCode;
import br.com.cardapio.domain.device.model.PairingId;

import java.time.Instant;
import java.util.Optional;

/**
 * Pedidos de vínculo. Ainda não pertencem a nenhum restaurante (o aparelho não sabe de qual
 * é), por isso estas operações não recebem establishmentId.
 */
public interface DevicePairingPort {

    void save(DevicePairing pairing);

    Optional<DevicePairing> findById(PairingId id);

    /** O pedido que ainda pode ser vinculado com este código (dentro da validade, sem aparelho). */
    Optional<DevicePairing> findOpenByCode(PairingCode code, Instant now);

    /**
     * Liga o pedido ao aparelho só se ele ainda estiver aberto, numa operação só no banco.
     * Devolve false se outro gerente usou o código antes ou se ele venceu agora há pouco.
     */
    boolean linkIfOpen(PairingId id, DeviceId deviceId, Instant now);

    /** Marca o token como entregue só se ainda não foi. Devolve false se outra busca chegou antes. */
    boolean claimIfLinked(PairingId id, Instant now);
}
