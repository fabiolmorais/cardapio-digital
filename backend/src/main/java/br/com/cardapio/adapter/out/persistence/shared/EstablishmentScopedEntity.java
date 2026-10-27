package br.com.cardapio.adapter.out.persistence.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Base de toda entidade que pertence a um restaurante: traz a coluna establishment_id e o
 * filtro do Hibernate. Com o filtro ligado (EstablishmentFilter), toda consulta destas
 * entidades ganha "AND establishment_id = ?" sozinha, inclusive a busca por id.
 */
@MappedSuperclass
@FilterDef(
        name = EstablishmentFilter.NAME,
        parameters = @ParamDef(name = EstablishmentFilter.PARAMETER, type = UUID.class),
        defaultCondition = "establishment_id = :" + EstablishmentFilter.PARAMETER,
        applyToLoadByKey = true)
@Filter(name = EstablishmentFilter.NAME)
public abstract class EstablishmentScopedEntity {

    @Column(name = "establishment_id", nullable = false, updatable = false)
    private UUID establishmentId;

    protected EstablishmentScopedEntity() {
    }

    protected EstablishmentScopedEntity(UUID establishmentId) {
        this.establishmentId = establishmentId;
    }

    public UUID getEstablishmentId() {
        return establishmentId;
    }
}
