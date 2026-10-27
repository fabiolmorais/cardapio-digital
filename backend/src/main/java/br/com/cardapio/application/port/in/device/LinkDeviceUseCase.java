package br.com.cardapio.application.port.in.device;

public interface LinkDeviceUseCase {

    DeviceSummary link(LinkDeviceCommand command);
}
