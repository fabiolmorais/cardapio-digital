package br.com.cardapio.application.port.out.floor;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;

import java.util.Optional;

public interface SectorPort {

    /** Grava um setor novo ou as mudanças de um existente. SectorNameTakenException se o nome repetir. */
    void save(Sector sector);

    /** Vazio se o setor não existe ou é de outro restaurante. */
    Optional<Sector> find(EstablishmentId establishmentId, SectorId id);

    /** Setores ativos, na ordem de exibição, com a contagem de mesas ativas. */
    ResultPage<SectorOverview> findActive(EstablishmentId establishmentId, PageQuery page);

    /** Já existe outro setor ativo com este nome (sem diferenciar maiúsculas)? exceptId: o próprio, ao renomear. */
    boolean activeNameExists(EstablishmentId establishmentId, String name, SectorId exceptId);

    /** Posição para um setor novo: depois de todos os que existem. */
    int nextPosition(EstablishmentId establishmentId);

    long countActiveTables(EstablishmentId establishmentId, SectorId sectorId);
}
