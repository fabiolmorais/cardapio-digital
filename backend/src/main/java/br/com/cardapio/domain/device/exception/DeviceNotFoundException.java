package br.com.cardapio.domain.device.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class DeviceNotFoundException extends NotFoundException {

    public DeviceNotFoundException() {
        super("Aparelho não encontrado");
    }
}
