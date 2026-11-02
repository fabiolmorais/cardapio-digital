package br.com.cardapio.application.port.in.call;

import java.util.List;

public interface ListTableCallsUseCase {

    /** Os chamados abertos da mesa: a página mostra "Garçom a caminho" mesmo depois de recarregar. */
    List<TableCall> list(String tableToken, String sessionToken);
}
