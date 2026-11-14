package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** A biblioteca de grupos de opções do restaurante (ADR 0010). */
public interface OptionGroupPort {

    /**
     * Grava o grupo e as opções dele. As opções que saíram vêm desativadas (nunca são apagadas:
     * os pedidos apontam para elas).
     */
    void save(OptionGroup group, List<OptionItem> items);

    /** O grupo (ativo ou não) com as opções ativas. */
    Optional<OptionGroupWithItems> find(EstablishmentId establishmentId, OptionGroupId id);

    /** Os grupos ativos, por nome. */
    ResultPage<OptionGroupWithItems> findActive(EstablishmentId establishmentId, PageQuery page);

    /** Os grupos ativos com estes ids (os que não existem, são de outro restaurante ou estão desativados, ficam de fora). */
    List<OptionGroup> findActiveByIds(EstablishmentId establishmentId, Collection<OptionGroupId> ids);

    long countActive(EstablishmentId establishmentId);

    Optional<OptionItem> findItem(EstablishmentId establishmentId, OptionItemId id);

    void saveItem(OptionItem item);
}
