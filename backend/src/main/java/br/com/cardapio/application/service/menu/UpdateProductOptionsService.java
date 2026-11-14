package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductOptionsForm;
import br.com.cardapio.application.port.in.menu.UpdateProductOptionsUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.application.port.out.menu.ProductOptionsChange;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.OptionItemNotFoundException;
import br.com.cardapio.domain.menu.exception.ProductVariantNotFoundException;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionGroupRules;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Salva os tamanhos e os grupos de um produto (ADR 0010). Os tamanhos que saíram são desativados,
 * nunca apagados (os pedidos apontam para eles). Os grupos precisam ser da biblioteca deste
 * restaurante: um id de outro restaurante dá 404, como em todo o sistema.
 */
public class UpdateProductOptionsService implements UpdateProductOptionsUseCase {

    private final ProductPort products;
    private final OptionGroupPort groups;
    private final ProductOptionsPort options;

    public UpdateProductOptionsService(ProductPort products, OptionGroupPort groups, ProductOptionsPort options) {
        this.products = products;
        this.groups = groups;
        this.options = options;
    }

    @Override
    @Transactional
    public ProductOptions update(EstablishmentId establishmentId, ProductId productId, ProductOptionsForm form) {
        Product product = MenuLookups.activeProduct(products, establishmentId, productId);
        List<ProductVariant> variants = variants(product, form.variants());
        List<ProductVariant> active = variants.stream().filter(ProductVariant::active).toList();

        // Os grupos: da biblioteca deste restaurante, ativos, sem repetir, até 10.
        if (form.groups().size() > OptionGroup.MAX_PER_PRODUCT) {
            throw new InvalidValueException("Um produto tem até " + OptionGroup.MAX_PER_PRODUCT + " grupos de opções");
        }
        List<OptionGroupId> groupIds = form.groups().stream().map(ProductOptionsForm.Group::groupId).toList();
        if (new LinkedHashSet<>(groupIds).size() != groupIds.size()) {
            throw new InvalidValueException("O mesmo grupo aparece duas vezes no produto");
        }
        Map<OptionGroupId, OptionGroupWithItems> linked = new HashMap<>();
        for (OptionGroupId id : groupIds) {
            linked.put(id, OptionLookups.activeGroup(groups, establishmentId, id));
        }

        List<ProductOptionsChange.Limit> limits = new ArrayList<>();
        for (ProductOptionsForm.Group g : form.groups()) {
            OptionGroup group = linked.get(g.groupId()).group();
            Set<Integer> seen = new HashSet<>();
            for (ProductOptionsForm.Limit limit : g.limits()) {
                ProductVariant variant = variantAt(active, limit.variant());
                if (!seen.add(limit.variant())) {
                    throw new InvalidValueException(group.title() + ": o limite do tamanho " + variant.name()
                            + " aparece duas vezes");
                }
                // O limite do tamanho segue as regras do grupo (um item dividido tem até 4 partes).
                OptionGroupRules rules = group.rules().limitedTo(limit.limit());
                if (rules.splitsItem() && rules.maxChoices() > OptionGroupRules.MAX_PARTS) {
                    throw new InvalidValueException(group.title() + ": um item dividido tem até "
                            + OptionGroupRules.MAX_PARTS + " partes");
                }
                limits.add(new ProductOptionsChange.Limit(group.id(), variant.id(), limit.limit()));
            }
        }

        // Os preços por tamanho: só de opções dos grupos ligados, um por opção e tamanho.
        Map<OptionItemId, OptionItem> items = linked.values().stream().flatMap(g -> g.items().stream())
                .collect(Collectors.toMap(OptionItem::id, Function.identity()));
        List<ProductOptionsChange.Price> prices = new ArrayList<>();
        Set<String> priced = new HashSet<>();
        for (ProductOptionsForm.Price price : form.prices()) {
            ProductVariant variant = variantAt(active, price.variant());
            if (!items.containsKey(price.optionId())) {
                throw new OptionItemNotFoundException();
            }
            if (!priced.add(price.optionId().value() + "/" + price.variant())) {
                throw new InvalidValueException(items.get(price.optionId()).name() + ": o preço do tamanho "
                        + variant.name() + " aparece duas vezes");
            }
            prices.add(new ProductOptionsChange.Price(price.optionId(), variant.id(), price.price()));
        }

        options.save(new ProductOptionsChange(establishmentId, productId, variants, groupIds, limits, prices));
        return options.find(establishmentId, productId);
    }

    /**
     * Os tamanhos do formulário (os novos ganham id; os que existiam são editados pelo id) e os que
     * saíram, desativados. No máximo um vale no rodízio; sem nenhum marcado, vale o primeiro.
     */
    private List<ProductVariant> variants(Product product, List<ProductOptionsForm.Variant> form) {
        if (form.size() > ProductVariant.MAX_PER_PRODUCT) {
            throw new InvalidValueException("Um produto tem até " + ProductVariant.MAX_PER_PRODUCT + " tamanhos");
        }
        Set<String> names = new HashSet<>();
        for (ProductOptionsForm.Variant v : form) {
            if (!names.add(v.name() == null ? "" : v.name().strip().toLowerCase(Locale.ROOT))) {
                throw new InvalidValueException("Há dois tamanhos com o nome \"" + v.name().strip() + "\"");
            }
        }
        if (form.stream().filter(ProductOptionsForm.Variant::rodizioDefault).count() > 1) {
            throw new InvalidValueException("Só um tamanho vale no rodízio");
        }
        Map<ProductVariantId, ProductVariant> current = options.findVariants(product.establishmentId(), product.id())
                .stream().filter(ProductVariant::active)
                .collect(Collectors.toMap(ProductVariant::id, Function.identity()));
        boolean anyRodizio = form.stream().anyMatch(ProductOptionsForm.Variant::rodizioDefault);
        List<ProductVariant> result = new ArrayList<>();
        Set<ProductVariantId> kept = new HashSet<>();
        for (int position = 0; position < form.size(); position++) {
            ProductOptionsForm.Variant v = form.get(position);
            ProductVariantId id;
            if (v.id() == null) {
                id = ProductVariantId.newId();
            } else if (current.containsKey(v.id()) && kept.add(v.id())) {
                id = v.id();
            } else {
                throw new ProductVariantNotFoundException();
            }
            boolean rodizio = anyRodizio ? v.rodizioDefault() : position == 0;
            result.add(new ProductVariant(id, product.establishmentId(), product.id(), v.name(), v.price(),
                    v.promoPrice(), v.available(), rodizio, position, true));
        }
        current.values().stream().filter(v -> !kept.contains(v.id())).map(ProductVariant::deactivate)
                .forEach(result::add);
        return result;
    }

    private static ProductVariant variantAt(List<ProductVariant> active, int index) {
        if (index < 0 || index >= active.size()) {
            throw new InvalidValueException("Os limites e os preços por tamanho precisam de um tamanho da lista");
        }
        return active.get(index);
    }
}
