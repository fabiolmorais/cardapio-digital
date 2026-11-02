package br.com.cardapio.application.port.in.menu;

public interface GetPublicMenuUseCase {

    /** O cardápio do restaurante dono da mesa deste token. TableNotFoundException se o token não abre mesa. */
    PublicMenu get(String tableToken);
}
