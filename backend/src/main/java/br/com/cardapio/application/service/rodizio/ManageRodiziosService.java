package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ManageRodiziosUseCase;
import br.com.cardapio.application.port.in.rodizio.RodizioForm;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.exception.RodizioLimitReachedException;
import br.com.cardapio.domain.rodizio.exception.RodizioNameTakenException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

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
        Rodizio current = rodizios.find(establishmentId, id).filter(Rodizio::active)
                .orElseThrow(RodizioNotFoundException::new);
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
