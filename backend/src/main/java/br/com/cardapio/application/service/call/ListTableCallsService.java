package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.ListTableCallsUseCase;
import br.com.cardapio.application.port.in.call.TableCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.call.model.CallType;
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

    /**
     * Os chamados abertos da mesa. O pedido de liberação é de um celular só: os dos outros não
     * aparecem (nem o código deles), senão quem abrisse o QR de longe veria o código de quem está
     * no salão.
     *
     * @Transactional sem readOnly: olhar os chamados também conta como uso, e renova a sessão.
     */
    @Override
    @Transactional
    public List<TableCall> list(String tableToken, String sessionToken) {
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, clock.instant());
        return calls.findOpenByTable(visit.table().establishmentId(), visit.table().id()).stream()
                .filter(view -> view.call().type() != CallType.UNLOCK
                        || view.call().sessionId().equals(visit.session().id()))
                .map(TableCall::from)
                .toList();
    }
}
