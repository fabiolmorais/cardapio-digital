package br.com.cardapio.application.port.in.device;

public interface CurrentDeviceUseCase {

    /** Identifica o aparelho pelo token do cookie; InvalidDeviceException se não estiver vinculado. */
    CurrentDevice current(String deviceToken);
}
