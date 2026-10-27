package br.com.cardapio.application.port.in.device;

/** O aparelho que está chamando e o restaurante dele (topo da tela do PIN). */
public record CurrentDevice(DeviceSummary device, String establishmentName) {
}
