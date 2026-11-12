package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Um rodízio do restaurante (Japonês, Pizza…): cobrado por pessoa, conforme a faixa, com os
 * produtos incluídos e as regras opcionais. É um cadastro: desativa, e não apaga.
 *
 * @param productIds os produtos incluídos (a aba Rodízio da mesa)
 * @param banner     o caminho do banner enviado pelo restaurante; null: a mesa mostra a imagem automática
 */
public record Rodizio(
        RodizioId id,
        EstablishmentId establishmentId,
        String name,
        RodizioType type,
        String description,
        RodizioRules rules,
        List<PriceTier> tiers,
        Set<ProductId> productIds,
        boolean active,
        Instant createdAt,
        String banner
) {

    /** Rodízios ativos por restaurante: Japonês, Pizza, almoço executivo… dez cobrem qualquer casa. */
    public static final int MAX_PER_ESTABLISHMENT = 10;
    public static final int MAX_TIERS = 6;

    public Rodizio {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        name = name == null ? "" : name.strip();
        if (name.isEmpty() || name.length() > 60) {
            throw new InvalidValueException("O rodízio precisa de um nome de até 60 letras.");
        }
        Objects.requireNonNull(type);
        description = description == null ? "" : description.strip();
        if (description.length() > 280) {
            throw new InvalidValueException("A descrição pode ter até 280 letras.");
        }
        Objects.requireNonNull(rules);
        tiers = List.copyOf(tiers);
        if (tiers.isEmpty() || tiers.size() > MAX_TIERS) {
            throw new InvalidValueException("O rodízio precisa de 1 a " + MAX_TIERS + " faixas de preço.");
        }
        Set<String> names = new HashSet<>();
        for (PriceTier tier : tiers) {
            if (!names.add(tier.name().toLowerCase(Locale.ROOT))) {
                throw new InvalidValueException("Duas faixas com o nome \"" + tier.name() + "\".");
            }
        }
        productIds = Set.copyOf(productIds);
        Objects.requireNonNull(createdAt);
    }

    public static Rodizio create(EstablishmentId establishmentId, String name, RodizioType type, String description,
                                 RodizioRules rules, List<PriceTier> tiers, Set<ProductId> productIds, Instant now) {
        return new Rodizio(RodizioId.newId(), establishmentId, name, type, description, rules, tiers, productIds, true,
                now, null);
    }

    /** Muda tudo de uma vez (o formulário do admin manda o rodízio inteiro). */
    public Rodizio edit(String newName, RodizioType newType, String newDescription, RodizioRules newRules,
                        List<PriceTier> newTiers, Set<ProductId> newProductIds) {
        return new Rodizio(id, establishmentId, newName, newType, newDescription, newRules, newTiers, newProductIds,
                active, createdAt, banner);
    }

    /** Pausa: sai das mesas (não pode ser pedido), mas continua no cadastro para voltar depois. */
    public Rodizio deactivate() {
        return new Rodizio(id, establishmentId, name, type, description, rules, tiers, productIds, false, createdAt,
                banner);
    }

    /** O mesmo rodízio, com o produto incluído (ou tirado): a chave na tela do produto. */
    public Rodizio withProduct(ProductId productId, boolean included) {
        Set<ProductId> next = new HashSet<>(productIds);
        if (included) {
            next.add(productId);
        } else {
            next.remove(productId);
        }
        return new Rodizio(id, establishmentId, name, type, description, rules, tiers, next, active, createdAt, banner);
    }

    /** Volta a valer para as mesas. */
    public Rodizio reactivate() {
        return new Rodizio(id, establishmentId, name, type, description, rules, tiers, productIds, true, createdAt,
                banner);
    }

    /**
     * O banner novo (ou nenhum, com null). Cada envio ganha um nome novo: a imagem velha não fica
     * presa no cache do celular.
     */
    public Rodizio withBanner(String path) {
        return new Rodizio(id, establishmentId, name, type, description, rules, tiers, productIds, active, createdAt,
                path);
    }

    /** O caminho de um banner novo deste rodízio. */
    public String newBannerPath() {
        return "rodizios/" + establishmentId.value() + "/" + id.value() + "/banner-" + UUID.randomUUID() + ".jpg";
    }
}
