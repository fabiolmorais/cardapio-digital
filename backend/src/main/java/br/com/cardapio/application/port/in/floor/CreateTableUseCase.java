package br.com.cardapio.application.port.in.floor;

public interface CreateTableUseCase {

    TableSummary create(CreateTableCommand command);
}
