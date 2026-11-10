package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TimedPrice;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O rodízio é gravado inteiro, como o admin o edita: a linha do rodízio e, no lugar das antigas,
 * as faixas, os preços especiais e os produtos incluídos. Trocar em vez de comparar linha a linha
 * é simples e seguro: ninguém aponta para uma faixa (a mesa copia o nome e o preço, no Guia 25).
 * Ler é sempre em quatro consultas, para um rodízio ou para a página toda (sem N+1).
 */
@Component
class RodizioPersistenceAdapter implements RodizioPort {

    private final RodizioJpaRepository rodizios;
    private final RodizioTierJpaRepository tiers;
    private final RodizioPriceJpaRepository prices;
    private final RodizioProductJpaRepository products;
    private final EstablishmentFilter establishmentFilter;

    RodizioPersistenceAdapter(RodizioJpaRepository rodizios, RodizioTierJpaRepository tiers,
                              RodizioPriceJpaRepository prices, RodizioProductJpaRepository products,
                              EstablishmentFilter establishmentFilter) {
        this.rodizios = rodizios;
        this.tiers = tiers;
        this.prices = prices;
        this.products = products;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(Rodizio r) {
        establishmentFilter.enable(r.establishmentId());
        UUID id = r.id().value();
        UUID establishment = r.establishmentId().value();
        RodizioRules rules = r.rules();
        rodizios.save(new RodizioJpaEntity(id, establishment, r.name(), r.type().name(), r.description(),
                rules.maxPerItemPerRound(), rules.maxPendingPerGuest(), rules.minRoundIntervalMinutes(),
                rules.timeLimitMinutes(), rules.wasteNotice(), r.active(), r.createdAt()));
        prices.deleteByRodizio(id);
        tiers.deleteByRodizio(id);
        products.deleteByRodizio(id);
        List<RodizioPriceJpaEntity> newPrices = new ArrayList<>();
        List<RodizioTierJpaEntity> newTiers = new ArrayList<>();
        for (int position = 0; position < r.tiers().size(); position++) {
            PriceTier tier = r.tiers().get(position);
            UUID tierId = UUID.randomUUID();
            newTiers.add(new RodizioTierJpaEntity(tierId, establishment, id, tier.name(),
                    tier.defaultPrice().cents(), position));
            for (int i = 0; i < tier.exceptions().size(); i++) {
                TimedPrice e = tier.exceptions().get(i);
                newPrices.add(new RodizioPriceJpaEntity(UUID.randomUUID(), establishment, tierId, toBits(e.days()),
                        e.startsAt(), e.endsAt(), e.price().cents(), i));
            }
        }
        tiers.saveAll(newTiers);
        prices.saveAll(newPrices);
        products.saveAll(r.productIds().stream()
                .map(p -> new RodizioProductJpaEntity(id, p.value(), establishment)).toList());
    }

    @Override
    public Optional<Rodizio> find(EstablishmentId establishmentId, RodizioId id) {
        establishmentFilter.enable(establishmentId);
        return rodizios.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(e -> toDomain(List.of(e)).getFirst());
    }

    @Override
    public ResultPage<Rodizio> findActive(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<RodizioJpaEntity> found = rodizios.findByEstablishmentIdAndActiveTrueOrderByNameAscIdAsc(
                establishmentId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(toDomain(found.getContent()), page.page(), page.size(), found.getTotalElements());
    }

    @Override
    public ResultPage<Rodizio> findAll(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<RodizioJpaEntity> found = rodizios.findByEstablishmentIdOrderByActiveDescNameAscIdAsc(
                establishmentId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(toDomain(found.getContent()), page.page(), page.size(), found.getTotalElements());
    }

    @Override
    public long countActive(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return rodizios.countByEstablishmentIdAndActiveTrue(establishmentId.value());
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, RodizioId exceptId) {
        establishmentFilter.enable(establishmentId);
        return rodizios.activeNameExists(establishmentId.value(), name, exceptId == null ? null : exceptId.value());
    }

    /** Os rodízios com as faixas, os preços e os produtos de todos eles: mais três consultas, no total. */
    private List<Rodizio> toDomain(List<RodizioJpaEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        Collection<UUID> ids = found.stream().map(RodizioJpaEntity::getId).toList();
        List<RodizioTierJpaEntity> allTiers = tiers.findByRodizioIdInOrderByPositionAsc(ids);
        Map<UUID, List<RodizioPriceJpaEntity>> pricesByTier = allTiers.isEmpty() ? Map.of()
                : prices.findByTierIdInOrderByPositionAsc(allTiers.stream().map(RodizioTierJpaEntity::getId).toList())
                        .stream().collect(Collectors.groupingBy(RodizioPriceJpaEntity::getTierId));
        Map<UUID, List<RodizioTierJpaEntity>> tiersByRodizio = allTiers.stream()
                .collect(Collectors.groupingBy(RodizioTierJpaEntity::getRodizioId));
        Map<UUID, Set<ProductId>> productsByRodizio = products.findByRodizioIdIn(ids).stream()
                .collect(Collectors.groupingBy(RodizioProductJpaEntity::getRodizioId,
                        Collectors.mapping(p -> new ProductId(p.getProductId()), Collectors.toSet())));
        return found.stream().map(e -> new Rodizio(new RodizioId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                e.getName(), RodizioType.valueOf(e.getType()), e.getDescription(),
                new RodizioRules(e.getMaxPerItemPerRound(), e.getMaxPendingPerGuest(), e.getMinRoundIntervalMinutes(),
                        e.getTimeLimitMinutes(), e.getWasteNotice()),
                tiersByRodizio.getOrDefault(e.getId(), List.of()).stream()
                        .map(t -> new PriceTier(t.getName(), Money.ofCents(t.getDefaultPriceCents()),
                                pricesByTier.getOrDefault(t.getId(), List.of()).stream()
                                        .map(p -> new TimedPrice(fromBits(p.getDays()), p.getStartsAt(),
                                                p.getEndsAt(), Money.ofCents(p.getPriceCents())))
                                        .toList()))
                        .toList(),
                productsByRodizio.getOrDefault(e.getId(), Set.of()), e.isActive(), e.getCreatedAt())).toList();
    }

    /** Segunda = bit 0 (1), terça = bit 1 (2) … domingo = bit 6 (64). */
    static short toBits(Set<DayOfWeek> days) {
        int bits = 0;
        for (DayOfWeek day : days) {
            bits |= 1 << (day.getValue() - 1);
        }
        return (short) bits;
    }

    static Set<DayOfWeek> fromBits(short bits) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if ((bits & (1 << (day.getValue() - 1))) != 0) {
                days.add(day);
            }
        }
        return days;
    }
}
