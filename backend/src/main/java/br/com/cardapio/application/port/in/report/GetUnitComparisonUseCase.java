package br.com.cardapio.application.port.in.report;

/** A tela "Unidades" dos relatórios (REL-80, Enterprise): só para quem é dono de todas as unidades. */
public interface GetUnitComparisonUseCase {

    UnitComparison get(UnitComparisonQuery query);
}
