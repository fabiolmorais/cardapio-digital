package br.com.cardapio.application.port.in.device;

import br.com.cardapio.domain.device.model.PairingId;

public interface ClaimPairingUseCase {

    /**
     * O aparelho pergunta se já foi vinculado. Segredo errado, pedido vencido sem vínculo ou
     * token já entregue: PairingNotFoundException.
     */
    ClaimResult claim(PairingId id, String secret);
}
