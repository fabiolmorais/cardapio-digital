package br.com.cardapio.application.port.in.floor;

public interface UpdateTableUseCase {

    TableSummary update(UpdateTableCommand command);
}
