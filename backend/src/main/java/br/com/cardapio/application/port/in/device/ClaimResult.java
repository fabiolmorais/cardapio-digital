package br.com.cardapio.application.port.in.device;

/**
 * Resultado da busca do token pelo aparelho: ou o gerente ainda não vinculou (Pending), ou
 * vinculou e o token sai agora, uma única vez (Linked).
 */
public sealed interface ClaimResult {

    record Pending() implements ClaimResult {
    }

    record Linked(String deviceToken, DeviceSummary device, String establishmentName) implements ClaimResult {

        @Override
        public String toString() {
            return "Linked[device=" + device.id() + "]";
        }
    }
}
