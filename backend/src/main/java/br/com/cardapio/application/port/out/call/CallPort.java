package br.com.cardapio.application.port.out.call;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CallPort {

    /**
     * Grava um chamado novo, a menos que a mesa já tenha um aberto do mesmo tipo (o índice único
     * decide, mesmo com dois toques ao mesmo tempo). false: já havia um, e nada foi gravado.
     */
    boolean insertIfNoneOpen(Call call);

    /** Quantos chamados este celular (a sessão) abriu DEPOIS daquele instante, cancelados também. */
    long countOpenedBySessionAfter(EstablishmentId establishmentId, TableSessionId sessionId, Instant since);

    /** Os chamados abertos deste celular (para cancelar quando a equipe o bloqueia). */
    List<Call> findOpenBySession(EstablishmentId establishmentId, TableSessionId sessionId);

    /**
     * Grava a mudança SÓ SE o chamado ainda estiver no estado esperado. false: alguém mudou o
     * chamado antes (outro garçom aceitou, a mesa cancelou), e nada foi gravado.
     */
    boolean update(Call changed, CallStatus expectedStatus);

    /** Vazio se o chamado não existe ou é de outro restaurante. */
    Optional<Call> find(EstablishmentId establishmentId, CallId id);

    Optional<CallView> findView(EstablishmentId establishmentId, CallId id);

    /** O chamado aberto deste tipo na mesa, se houver (no máximo um). */
    Optional<Call> findOpen(EstablishmentId establishmentId, DiningTableId tableId, CallType type);

    /** Os chamados abertos da mesa (no máximo um de cada tipo), do mais antigo para o mais novo. */
    List<CallView> findOpenByTable(EstablishmentId establishmentId, DiningTableId tableId);

    /** A fila da equipe: os chamados abertos do restaurante, por ordem de chegada. */
    ResultPage<CallView> findQueue(EstablishmentId establishmentId, PageQuery page);
}
