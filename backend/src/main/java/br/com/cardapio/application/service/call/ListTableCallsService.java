package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.ListTableCallsUseCase;
import br.com.cardapio.application.port.in.call.TableCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

public class ListTableCallsService implements ListTableCallsUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final CallPort calls;
    private final Clock clock;

    public ListTableCallsService(DiningTablePort tables, TableSessionPort sessions, CallPort calls, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.calls = calls;
        this.clock = clock;
    }

    /** @Transactional sem readOnly: olhar os chamados também conta como uso, e renova a sessão. */
    @Override
    @Transactional
    public List<TableCall> list(String tableToken, String sessionToken) {
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, clock.instant());
        return calls.findOpenByTable(visit.table().establishmentId(), visit.table().id()).stream()
                .map(TableCall::from)
                .toList();
    }
}
