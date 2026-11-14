package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.menu.exception.ChoiceUnavailableException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Os tamanhos e os grupos de opções de um produto, e a regra do preço (ADR 0010).
 *
 * <p>Preço do item = preço base + a parte de cada grupo. O preço base é o do tamanho escolhido (ou o
 * do produto, sem tamanhos), já com a promoção; a parte de cada grupo segue o "como cobra" dele:
 * soma (menos as escolhas grátis), maior preço ou média arredondada para cima no centavo. Quem
 * calcula é sempre o back: o celular só mostra a conta.
 *
 * <p>Só entram aqui os tamanhos, grupos e opções ativos. Produto sem nada disso fica com
 * {@link #isEmpty()} e se comporta exatamente como antes.
 *
 * @param variants os tamanhos, na ordem
 * @param groups   os grupos ligados, na ordem em que o cliente vê
 * @param prices   o preço de uma opção num tamanho, quando é diferente do preço da opção
 */
public record ProductOptions(ProductId productId, List<ProductVariant> variants, List<LinkedGroup> groups,
                             Map<PriceKey, Money> prices) {

    /**
     * Um grupo ligado ao produto, com as opções ativas dele e os limites por tamanho.
     *
     * @param limits o mínimo e o máximo do grupo em cada tamanho, quando mudam
     */
    public record LinkedGroup(OptionGroup group, List<OptionItem> items, Map<ProductVariantId, ChoiceLimit> limits) {

        public LinkedGroup {
            Objects.requireNonNull(group);
            items = items.stream().filter(OptionItem::active)
                    .sorted(Comparator.comparingInt(OptionItem::position)).toList();
            limits = Map.copyOf(limits);
        }
    }

    /** O preço de uma opção num tamanho. */
    public record PriceKey(OptionItemId optionId, ProductVariantId variantId) {
    }

    public ProductOptions {
        Objects.requireNonNull(productId);
        variants = variants.stream().filter(ProductVariant::active)
                .sorted(Comparator.comparingInt(ProductVariant::position)).toList();
        groups = groups.stream().filter(g -> g.group().active()).toList();
        prices = Map.copyOf(prices);
    }

    public static ProductOptions none(ProductId productId) {
        return new ProductOptions(productId, List.of(), List.of(), Map.of());
    }

    /** Sem tamanhos nem grupos: o produto de sempre, com o "Adicionar" de um toque. */
    public boolean isEmpty() {
        return variants.isEmpty() && groups.isEmpty();
    }

    /** O tamanho que vale no rodízio: o marcado, ou o primeiro. Null sem tamanhos. */
    public ProductVariant rodizioVariant() {
        return variants.stream().filter(ProductVariant::rodizioDefault).findFirst()
                .orElse(variants.isEmpty() ? null : variants.getFirst());
    }

    /** O preço da opção neste tamanho (sem tamanho, ou sem preço próprio no tamanho, o da opção). */
    public Money priceOf(OptionItem option, ProductVariant variant) {
        if (variant == null) {
            return option.price();
        }
        return prices.getOrDefault(new PriceKey(option.id(), variant.id()), option.price());
    }

    /** As regras do grupo neste tamanho (com o mínimo e o máximo do tamanho, se houver). */
    public OptionGroupRules rulesOf(LinkedGroup linked, ProductVariant variant) {
        return linked.group().rules().limitedTo(variant == null ? null : linked.limits().get(variant.id()));
    }

    /**
     * O preço de uma unidade do item com estas escolhas, conferindo tudo: o tamanho, os grupos
     * obrigatórios, os máximos, o que esgotou e o que saiu do cardápio.
     *
     * @param product o produto (o nome, para as mensagens, e o preço base, sem tamanhos)
     * @param round   uma rodada do rodízio: o preço é zero, o tamanho é o do rodízio, e os
     *                adicionais pagos (opções com preço em grupos que somam) não entram
     */
    public PricedSelection price(ProductDetails product, OptionSelection selection, boolean round) {
        Objects.requireNonNull(product);
        Objects.requireNonNull(selection);
        String productName = product.name();
        ProductVariant variant = chooseVariant(productName, selection.variantId(), round);
        Money base = round ? Money.ZERO : variant != null ? variant.currentPrice() : product.currentPrice();

        // A mesma opção mandada duas vezes vale como uma, somando as vezes.
        Map<OptionItemId, Integer> wanted = new LinkedHashMap<>();
        selection.options().forEach(o -> wanted.merge(o.optionId(), o.quantity(), Integer::sum));
        Map<OptionItemId, OptionItem> known = new LinkedHashMap<>();
        groups.forEach(g -> g.items().forEach(i -> known.put(i.id(), i)));
        for (OptionItemId id : wanted.keySet()) {
            if (!known.containsKey(id)) {
                throw new ChoiceUnavailableException(productName, null);
            }
        }

        Money total = base;
        List<ChosenOption> chosen = new ArrayList<>();
        for (LinkedGroup linked : groups) {
            OptionGroupRules rules = rulesOf(linked, variant);
            List<OptionItem> picked = linked.items().stream().filter(i -> wanted.containsKey(i.id())).toList();
            int count = picked.stream().mapToInt(i -> wanted.get(i.id())).sum();
            if (count < rules.minChoices() || count > rules.maxChoices()) {
                throw new InvalidValueException(productName + " · " + linked.group().title() + ": "
                        + ruleText(rules) + ".");
            }
            for (OptionItem item : picked) {
                if (wanted.get(item.id()) > rules.maxPerOption()) {
                    throw new InvalidValueException(productName + " · " + item.name() + ": até "
                            + rules.maxPerOption() + (rules.maxPerOption() == 1 ? " vez." : " vezes."));
                }
                if (!item.available()) {
                    throw new ChoiceUnavailableException(productName, item.name());
                }
            }
            List<Money> unitPrices = picked.stream().map(i -> priceOf(i, variant)).toList();
            List<Integer> quantities = picked.stream().map(i -> wanted.get(i.id())).toList();
            List<Money> charged;
            if (round) {
                for (int i = 0; i < picked.size(); i++) {
                    if (rules.pricing() == OptionPricing.SUM && !unitPrices.get(i).isZero()) {
                        throw new InvalidValueException(productName + " · " + picked.get(i).name()
                                + " é um adicional pago e não entra no rodízio. Peça no cardápio.");
                    }
                }
                charged = picked.stream().map(i -> Money.ZERO).toList();
            } else {
                charged = charge(rules, unitPrices, quantities);
            }
            for (int i = 0; i < picked.size(); i++) {
                OptionItem item = picked.get(i);
                chosen.add(new ChosenOption(linked.group().id(), linked.group().title(), rules.splitsItem(), item.id(),
                        item.name(), quantities.get(i), unitPrices.get(i), charged.get(i)));
                total = total.plus(charged.get(i));
            }
        }
        ItemChoices choices = new ItemChoices(variant == null ? null : variant.id(),
                variant == null ? null : variant.name(), chosen);
        return new PricedSelection(total, choices);
    }

    /**
     * O menor preço possível do produto, com o mínimo obrigatório de cada grupo: o "a partir de" do
     * cardápio. Conta só o que está disponível (o que esgotou não dá para escolher).
     */
    public Money fromPrice(ProductDetails product) {
        List<ProductVariant> candidates = variants.stream().filter(ProductVariant::available).toList();
        if (candidates.isEmpty()) {
            candidates = variants;
        }
        if (candidates.isEmpty()) {
            return cheapest(product.currentPrice(), null);
        }
        return candidates.stream().map(v -> cheapest(v.currentPrice(), v)).min(Money::compareTo).orElseThrow();
    }

    private Money cheapest(Money base, ProductVariant variant) {
        Money total = base;
        for (LinkedGroup linked : groups) {
            OptionGroupRules rules = rulesOf(linked, variant);
            if (!rules.required()) {
                continue;
            }
            // As unidades mais baratas que dá para escolher (cada opção até o máximo de vezes).
            List<Money> units = linked.items().stream().filter(OptionItem::available)
                    .flatMap(i -> IntStream.range(0, rules.maxPerOption()).mapToObj(n -> priceOf(i, variant)))
                    .sorted().limit(rules.minChoices()).toList();
            if (units.size() < rules.minChoices()) {
                continue; // faltam opções: o grupo nem dá para completar agora
            }
            total = total.plus(charge(rules, units, units.stream().map(u -> 1).toList()).stream()
                    .reduce(Money.ZERO, Money::plus));
        }
        return total;
    }

    /** Quanto cada opção entra no preço, pela regra do grupo. */
    static List<Money> charge(OptionGroupRules rules, List<Money> prices, List<Integer> quantities) {
        int n = prices.size();
        List<Money> charged = new ArrayList<>(IntStream.range(0, n).mapToObj(i -> Money.ZERO).toList());
        if (n == 0) {
            return charged;
        }
        switch (rules.pricing()) {
            case SUM -> {
                // As escolhas grátis ficam com as unidades mais baratas.
                int[] free = new int[n];
                int left = rules.freeQuantity();
                List<Integer> byPrice = IntStream.range(0, n).boxed()
                        .sorted(Comparator.comparing(prices::get)).toList();
                for (int i : byPrice) {
                    int take = Math.min(left, quantities.get(i));
                    free[i] = take;
                    left -= take;
                }
                for (int i = 0; i < n; i++) {
                    charged.set(i, prices.get(i).times(quantities.get(i) - free[i]));
                }
            }
            case HIGHEST -> {
                int top = IntStream.range(0, n).boxed().max(Comparator.comparing(prices::get)).orElseThrow();
                charged.set(top, prices.get(top));
            }
            case AVERAGE -> {
                long sum = 0;
                int count = 0;
                for (int i = 0; i < n; i++) {
                    sum += prices.get(i).cents() * quantities.get(i);
                    count += quantities.get(i);
                }
                long average = Math.ceilDiv(sum, count);
                // Cada opção entra com a parte dela; o que sobra do arredondamento vai na primeira.
                long given = 0;
                for (int i = 0; i < n; i++) {
                    long part = prices.get(i).cents() * quantities.get(i) / count;
                    charged.set(i, Money.ofCents(part));
                    given += part;
                }
                charged.set(0, charged.getFirst().plus(Money.ofCents(average - given)));
            }
        }
        return charged;
    }

    private ProductVariant chooseVariant(String productName, ProductVariantId wanted, boolean round) {
        if (variants.isEmpty()) {
            if (wanted != null && !round) {
                throw new ChoiceUnavailableException(productName, null);
            }
            return null;
        }
        if (round) {
            return rodizioVariant();
        }
        if (wanted == null) {
            throw new InvalidValueException(productName + ": escolha o tamanho.");
        }
        ProductVariant variant = variants.stream().filter(v -> v.id().equals(wanted)).findFirst()
                .orElseThrow(() -> new ChoiceUnavailableException(productName, null));
        if (!variant.available()) {
            throw new ChoiceUnavailableException(productName, "o tamanho " + variant.name());
        }
        return variant;
    }

    /** "escolha 1", "escolha até 2", "escolha de 1 a 3". */
    static String ruleText(OptionGroupRules rules) {
        if (rules.minChoices() == rules.maxChoices()) {
            return "escolha " + rules.maxChoices();
        }
        if (rules.minChoices() == 0) {
            return "escolha até " + rules.maxChoices();
        }
        return "escolha de " + rules.minChoices() + " a " + rules.maxChoices();
    }
}
