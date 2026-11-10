package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ManageRodiziosUseCase;
import br.com.cardapio.application.port.in.rodizio.RodizioForm;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.ProductNotFoundException;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.exception.RodizioLimitReachedException;
import br.com.cardapio.domain.rodizio.exception.RodizioNameTakenException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ManageRodiziosService implements ManageRodiziosUseCase {

    private final RodizioPort rodizios;
    private final ProductPort products;
    private final Clock clock;

    public ManageRodiziosService(RodizioPort rodizios, ProductPort products, Clock clock) {
        this.rodizios = rodizios;
        this.products = products;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Rodizio create(EstablishmentId establishmentId, RodizioForm form) {
        if (rodizios.countActive(establishmentId) >= Rodizio.MAX_PER_ESTABLISHMENT) {
            throw new RodizioLimitReachedException();
        }
        checkNameAndProducts(establishmentId, form, null);
        Rodizio created = Rodizio.create(establishmentId, form.name(), form.type(), form.description(), form.rules(),
                form.tiers(), form.productIds(), clock.instant());
        rodizios.save(created);
        return created;
    }

    @Override
    @Transactional
    public Rodizio update(EstablishmentId establishmentId, RodizioId id, RodizioForm form) {
        // O pausado também se edita (o nome só não pode repetir o de um ativo).
        Rodizio current = rodizios.find(establishmentId, id).orElseThrow(RodizioNotFoundException::new);
        checkNameAndProducts(establishmentId, form, id);
        Rodizio edited = current.edit(form.name(), form.type(), form.description(), form.rules(), form.tiers(),
                form.productIds());
        rodizios.save(edited);
        return edited;
    }

    @Override
    @Transactional
    public void deactivate(EstablishmentId establishmentId, RodizioId id) {
        Rodizio current = rodizios.find(establishmentId, id).orElseThrow(RodizioNotFoundException::new);
        if (current.active()) {
            rodizios.save(current.deactivate());
        }
    }

    @Override
    @Transactional
    public Rodizio reactivate(EstablishmentId establishmentId, RodizioId id) {
        Rodizio current = rodizios.find(establishmentId, id).orElseThrow(RodizioNotFoundException::new);
        if (current.active()) {
            return current;
        }
        if (rodizios.countActive(establishmentId) >= Rodizio.MAX_PER_ESTABLISHMENT) {
            throw new RodizioLimitReachedException();
        }
        if (rodizios.activeNameExists(establishmentId, current.name(), id)) {
            throw new RodizioNameTakenException(current.name());
        }
        Rodizio reactivated = current.reactivate();
        rodizios.save(reactivated);
        return reactivated;
    }

    @Override
    @Transactional
    public Set<RodizioId> setProductRodizios(EstablishmentId establishmentId, ProductId productId,
                                             Set<RodizioId> rodizioIds) {
        Product product = products.find(establishmentId, productId).filter(Product::active)
                .orElseThrow(ProductNotFoundException::new);
        List<Rodizio> all = rodizios.findAll(establishmentId, new PageQuery(0, PageQuery.MAX_SIZE)).content();
        Set<RodizioId> known = all.stream().map(Rodizio::id).collect(Collectors.toSet());
        // Um id que não é rodízio deste restaurante (de outro, ou inventado): 404, sem mexer em nada.
        if (!known.containsAll(rodizioIds)) {
            throw new RodizioNotFoundException();
        }
        Set<RodizioId> included = new HashSet<>();
        for (Rodizio rodizio : all) {
            boolean wanted = rodizioIds.contains(rodizio.id());
            if (wanted) {
                included.add(rodizio.id());
            }
            if (wanted != rodizio.productIds().contains(product.id())) {
                rodizios.save(rodizio.withProduct(product.id(), wanted));
            }
        }
        return included;
    }

    /**
     * O nome não pode repetir o de outro rodízio ativo, e os produtos incluídos precisam ser do
     * cardápio DESTE restaurante (e ativos). Um id de produto de outro restaurante, mandado à mão,
     * cai aqui: a consulta só acha os produtos do restaurante de quem está logado.
     */
    private void checkNameAndProducts(EstablishmentId establishmentId, RodizioForm form, RodizioId exceptId) {
        String name = form.name() == null ? "" : form.name().strip();
        if (rodizios.activeNameExists(establishmentId, name, exceptId)) {
            throw new RodizioNameTakenException(name);
        }
        if (!form.productIds().isEmpty()
                && products.findActiveByIds(establishmentId, form.productIds()).size() != form.productIds().size()) {
            throw new InvalidValueException("Algum dos produtos escolhidos não está no cardápio. Atualize a tela.");
        }
    }
}
