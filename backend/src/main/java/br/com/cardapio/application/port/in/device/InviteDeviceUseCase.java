package br.com.cardapio.application.port.in.device;

/** O gerente vincula um aparelho novo por QR Code: o aparelho só precisa escanear. */
public interface InviteDeviceUseCase {

    DeviceInvite invite(InviteDeviceCommand command);
}
