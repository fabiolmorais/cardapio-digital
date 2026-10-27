package br.com.cardapio.application.port.out.device;

import br.com.cardapio.domain.device.model.Device;

/** O aparelho junto com o nome do restaurante dele (que o aparelho mostra na tela). */
public record LinkedDevice(Device device, String establishmentName) {
}
