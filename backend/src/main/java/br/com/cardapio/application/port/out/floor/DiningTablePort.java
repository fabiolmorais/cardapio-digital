package br.com.cardapio.application.port.out.floor;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;

import java.util.Optional;

public interface DiningTablePort {

    /** Grava uma mesa nova ou as mudanças de uma existente. TableLabelTakenException se o rótulo repetir. */
    void save(DiningTable table);

    /** Vazio se a mesa não existe ou é de outro restaurante. */
    Optional<DiningTable> find(EstablishmentId establishmentId, DiningTableId id);

    /** Mesas do restaurante (de um setor só, se sectorId vier), ativas primeiro, por rótulo. */
    ResultPage<DiningTable> findAll(EstablishmentId establishmentId, SectorId sectorId, PageQuery page);

    /** Já existe outra mesa com este rótulo (ativa ou não)? exceptId: a própria, ao renomear. */
    boolean labelExists(EstablishmentId establishmentId, String label, DiningTableId exceptId);
}
