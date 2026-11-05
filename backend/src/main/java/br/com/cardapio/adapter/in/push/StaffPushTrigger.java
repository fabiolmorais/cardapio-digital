package br.com.cardapio.adapter.in.push;

import br.com.cardapio.application.port.in.push.NotifyStaffUseCase;
import br.com.cardapio.domain.shared.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dispara as notificações no celular depois que a mudança foi gravada (AFTER_COMMIT), numa thread
 * virtual: o pedido do cliente não espera o serviço de push. O evento chega daqui da instância que
 * fez a mudança (e não do LISTEN do Postgres, que todas as instâncias recebem): assim, cada aviso
 * sai uma vez só.
 */
@Component
class StaffPushTrigger {

    private static final Logger LOG = LoggerFactory.getLogger(StaffPushTrigger.class);

    private final NotifyStaffUseCase notifyStaff;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    StaffPushTrigger(NotifyStaffUseCase notifyStaff) {
        this.notifyStaff = notifyStaff;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void on(DomainEvent event) {
        executor.execute(() -> {
            try {
                notifyStaff.notify(event);
            } catch (RuntimeException e) {
                // A notificação é um extra: falhar aqui não desfaz o chamado nem o pedido.
                LOG.warn("Falha ao avisar a equipe no celular: {}", e.getMessage());
            }
        });
    }
}
