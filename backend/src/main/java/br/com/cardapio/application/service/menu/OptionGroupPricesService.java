package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.GetOptionGroupPricesUseCase;
import br.com.cardapio.application.port.in.menu.OptionGroupPrices;
import br.com.cardapio.application.port.in.menu.UpdateOptionGroupPricesUseCase;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.OptionGroupWithItems;
import br.com.cardapio.application.port.out.menu.OptionPriceChange;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A grade de preços de um grupo (complemento da ADR 0010): o preço de cada opção em cada tamanho
 * dos produtos que usam o grupo. As colunas são os tamanhos ativos de cada produto; o produto sem
 * tamanhos é uma coluna só (o preço "neste produto"). Num produto com tamanhos não há preço
 * "neste produto": ali ele não vale.
 */
public class OptionGroupPricesService implements GetOptionGroupPricesUseCase, UpdateOptionGroupPricesUseCase {

    private final OptionGroupPort groups;
    private final ProductPort products;
    private final ProductOptionsPort options;

    public OptionGroupPricesService(OptionGroupPort groups, ProductPort products, ProductOptionsPort options) {
        this.groups = groups;
        this.products = products;
        this.options = options;
    }

    @Override
    @Transactional(readOnly = true)
    public OptionGroupPrices get(EstablishmentId establishmentId, OptionGroupId groupId) {
        return grid(establishmentId, OptionLookups.activeGroup(groups, establishmentId, groupId));
    }

    @Override
    @Transactional
    public OptionGroupPrices update(EstablishmentId establishmentId, OptionGroupId groupId,
                                    List<OptionGroupPrices.Price> prices) {
        OptionGroupWithItems group = OptionLookups.activeGroup(groups, establishmentId, groupId);
        OptionGroupPrices current = grid(establishmentId, group);
        Map<OptionItemId, OptionItem> items = group.items().stream()
                .collect(Collectors.toMap(OptionItem::id, Function.identity()));
        Set<String> columns = current.columns().stream().map(c -> key(c.productId(), c.variantId()))
                .collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        List<OptionPriceChange.Price> accepted = new ArrayList<>();
        for (OptionGroupPrices.Price price : prices) {
            OptionItem item = items.get(price.optionId());
            if (item == null) {
                throw new InvalidValueException("Uma das opções não é deste grupo");
            }
            if (!columns.contains(key(price.productId(), price.variantId()))) {
                throw new InvalidValueException(item.name() + ": o produto ou o tamanho não usa este grupo");
            }
            if (price.price().compareTo(ProductDetails.PRICE_MAX) > 0) {
                throw new InvalidValueException("Preço alto demais (máximo R$ 99.999,99)");
            }
            if (!seen.add(key(price.productId(), price.variantId()) + "/" + price.optionId().value())) {
                throw new InvalidValueException(item.name() + ": o mesmo preço aparece duas vezes");
            }
            accepted.add(new OptionPriceChange.Price(price.productId(), price.variantId(), price.optionId(),
                    price.price()));
        }
        options.replaceOptionPrices(new OptionPriceChange(establishmentId, items.keySet(), accepted));
        return grid(establishmentId, group);
    }

    /** As colunas (os produtos ativos que usam o grupo, na ordem do cardápio) e os preços preenchidos. */
    private OptionGroupPrices grid(EstablishmentId establishmentId, OptionGroupWithItems group) {
        Map<ProductId, ProductOptions> configured = options.findAllActive(establishmentId);
        Set<OptionItemId> mine = group.items().stream().map(OptionItem::id).collect(Collectors.toSet());
        List<OptionGroupPrices.Column> columns = new ArrayList<>();
        List<OptionGroupPrices.Price> prices = new ArrayList<>();
        for (Product product : products.findAllActive(establishmentId)) {
            ProductOptions choices = configured.get(product.id());
            if (choices == null || choices.groups().stream().noneMatch(l -> l.group().id().equals(group.group().id()))) {
                continue;
            }
            String name = product.details().name();
            if (choices.variants().isEmpty()) {
                columns.add(new OptionGroupPrices.Column(product.id(), name, null, null));
            } else {
                for (ProductVariant variant : choices.variants()) {
                    columns.add(new OptionGroupPrices.Column(product.id(), name, variant.id(), variant.name()));
                }
            }
            Set<Object> valid = new HashSet<>();
            if (choices.variants().isEmpty()) {
                valid.add("produto");
            } else {
                choices.variants().forEach(v -> valid.add(v.id()));
            }
            choices.prices().forEach((k, price) -> {
                Object column = k.variantId() == null ? "produto" : k.variantId();
                if (mine.contains(k.optionId()) && valid.contains(column)) {
                    prices.add(new OptionGroupPrices.Price(product.id(), k.variantId(), k.optionId(), price));
                }
            });
        }
        return new OptionGroupPrices(columns, prices);
    }

    private static String key(ProductId productId, Object variantId) {
        return productId.value() + "/" + (variantId == null ? "produto" : variantId);
    }
}
