package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.domain.device.model.PairingCode;

import java.time.Instant;

/** Sorteia o código de 6 números de um pedido de vínculo. */
final class PairingCodes {

    /** Com 1 milhão de códigos possíveis, repetir um aberto é raro; 5 tentativas sobram. */
    private static final int MAX_ATTEMPTS = 5;

    private PairingCodes() {
    }

    /** Um código que nenhum outro pedido aberto esteja usando agora. */
    static PairingCode free(DevicePairingPort pairings, RandomCodeGenerator codes, Instant now) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            PairingCode candidate = new PairingCode(codes.digits(PairingCode.LENGTH));
            if (pairings.findOpenByCode(candidate, now).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código livre");
    }
}
