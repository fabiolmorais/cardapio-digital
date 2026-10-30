package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryPort {

    /** Grava uma categoria nova ou as mudanças de uma existente. MenuCategoryNameTakenException se o nome repetir. */
    void save(MenuCategory category);

    /** Vazio se a categoria não existe ou é de outro restaurante. */
    Optional<MenuCategory> find(EstablishmentId establishmentId, MenuCategoryId id);

    /** Categorias ativas, na ordem de exibição, com a contagem de produtos ativos (para a tela). */
    ResultPage<MenuCategoryOverview> findOverview(EstablishmentId establishmentId, PageQuery page);

    /** Todas as categorias ativas, na ordem (para reordenar: aqui precisa da lista inteira). */
    List<MenuCategory> findActive(EstablishmentId establishmentId);

    /** Já existe outra categoria ativa com este nome (sem diferenciar maiúsculas)? exceptId: a própria, ao renomear. */
    boolean activeNameExists(EstablishmentId establishmentId, String name, MenuCategoryId exceptId);

    /** Posição para uma categoria nova: depois de todas as que existem. */
    int nextPosition(EstablishmentId establishmentId);

    long countActiveProducts(EstablishmentId establishmentId, MenuCategoryId categoryId);
}
