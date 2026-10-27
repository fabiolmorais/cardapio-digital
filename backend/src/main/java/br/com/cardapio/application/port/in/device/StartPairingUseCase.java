package br.com.cardapio.application.port.in.device;

public interface StartPairingUseCase {

    /** Abre um pedido de vínculo para um aparelho novo (público: o aparelho ainda não é ninguém). */
    PairingStarted start();
}
