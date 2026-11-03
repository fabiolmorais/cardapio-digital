package br.com.cardapio.application.port.in.order;

public interface ListTableOrdersUseCase {

    TableOrders list(String tableToken, String sessionToken);
}
