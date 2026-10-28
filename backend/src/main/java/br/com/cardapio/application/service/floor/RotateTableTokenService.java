package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.RotateTableTokenUseCase;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.TableToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class RotateTableTokenService implements RotateTableTokenUseCase {

    private final DiningTablePort tables;
    private final RandomTokenGenerator tokens;
    private final Clock clock;

    public RotateTableTokenService(DiningTablePort tables, RandomTokenGenerator tokens, Clock clock) {
        this.tables = tables;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TableSummary rotate(EstablishmentId establishmentId, DiningTableId tableId) {
        DiningTable rotated = tables.find(establishmentId, tableId)
                .orElseThrow(TableNotFoundException::new)
                .rotateToken(new TableToken(tokens.generate()), clock.instant());
        tables.save(rotated);
        return TableSummary.from(rotated);
    }
}
