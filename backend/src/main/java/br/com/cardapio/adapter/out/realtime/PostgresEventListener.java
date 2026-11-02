package br.com.cardapio.adapter.out.realtime;

import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.event.DomainEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Ouve o canal do NOTIFY e entrega cada evento a quem, NESTA instância, assinou o restaurante
 * dele. Com várias instâncias do back, todas ouvem: o aviso publicado numa chega às telas
 * ligadas em qualquer uma.
 *
 * Usa uma conexão própria, fora do pool: ela fica parada no LISTEN o tempo todo e não pode
 * voltar para o pool nem ser emprestada a uma consulta. Se cair, reconecta; e encerra as
 * assinaturas, porque os avisos do intervalo se perderam: as telas reconectam e buscam tudo.
 */
@Component
class PostgresEventListener implements EventStreamPort, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PostgresEventListener.class);
    /** Quanto o getNotifications espera por avisos antes de voltar e conferir se é hora de parar. */
    private static final int POLL_MILLIS = 1000;
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    record Subscriber(Consumer<DomainEvent> onEvent, Runnable onEnd) {
    }

    private final JdbcConnectionDetails database;
    private final RealtimeMessages messages;
    private final Clock clock;
    private final Timer delay;
    private final Map<EstablishmentId, Set<Subscriber>> subscribers = new ConcurrentHashMap<>();
    /** Cada entrega numa thread virtual: uma tela lenta não atrasa as outras. */
    private final ExecutorService delivery = Executors.newVirtualThreadPerTaskExecutor();
    private final CountDownLatch firstListen = new CountDownLatch(1);
    private volatile boolean running;
    private Thread thread;

    PostgresEventListener(JdbcConnectionDetails database, RealtimeMessages messages, Clock clock,
                          MeterRegistry meters) {
        this.database = database;
        this.messages = messages;
        this.clock = clock;
        // A métrica do CLAUDE.md: do momento do chamado até o aviso sair para as telas.
        this.delay = Timer.builder("realtime.event.delay")
                .description("Tempo entre o evento acontecer e ser entregue às telas desta instância")
                .register(meters);
    }

    @Override
    public Runnable subscribe(EstablishmentId establishmentId, Consumer<DomainEvent> onEvent, Runnable onEnd) {
        Subscriber subscriber = new Subscriber(onEvent, onEnd);
        subscribers.computeIfAbsent(establishmentId, id -> ConcurrentHashMap.newKeySet()).add(subscriber);
        return () -> subscribers.computeIfPresent(establishmentId, (id, set) -> {
            set.remove(subscriber);
            return set.isEmpty() ? null : set;
        });
    }

    /** Liga junto com a aplicação, e só a dá por pronta depois do primeiro LISTEN (ou de 10 s). */
    @Override
    public void start() {
        running = true;
        thread = Thread.ofPlatform().name("realtime-listener").daemon().start(this::listen);
        try {
            if (!firstListen.await(10, TimeUnit.SECONDS)) {
                log.warn("O tempo real ainda não conseguiu ouvir o banco; segue tentando");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Desliga antes do servidor web: as telas abertas são encerradas e o desligamento não espera por elas. */
    @Override
    public void stop() {
        running = false;
        endAll();
        delivery.shutdown();
        if (thread != null) {
            try {
                thread.join(POLL_MILLIS * 2L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void listen() {
        Duration backoff = Duration.ofSeconds(1);
        while (running) {
            try (Connection connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(),
                    database.getPassword());
                 Statement statement = connection.createStatement()) {
                statement.execute("LISTEN " + RealtimeMessages.CHANNEL);
                firstListen.countDown();
                backoff = Duration.ofSeconds(1);
                PGConnection postgres = connection.unwrap(PGConnection.class);
                while (running) {
                    PGNotification[] notifications = postgres.getNotifications(POLL_MILLIS);
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            dispatch(notification.getParameter());
                        }
                    }
                }
            } catch (SQLException e) {
                if (!running) {
                    return;
                }
                // Sem a mensagem do driver no log: ela pode trazer o endereço do banco.
                log.warn("O tempo real perdeu a conexão com o banco ({}); reconectando em {} s", e.getSQLState(),
                        backoff.toSeconds());
                endAll();
                sleep(backoff);
                backoff = backoff.multipliedBy(2).compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff.multipliedBy(2);
            }
        }
    }

    private void dispatch(String text) {
        if (!running) {
            return; // desligando: a entrega já foi encerrada
        }
        DomainEvent event;
        try {
            event = messages.fromJson(text);
        } catch (RuntimeException e) {
            log.warn("Aviso de tempo real ignorado: {}", e.getMessage());
            return;
        }
        delay.record(Duration.between(event.occurredAt(), clock.instant()));
        for (Subscriber subscriber : subscribers.getOrDefault(event.establishmentId(), Set.of())) {
            delivery.execute(() -> {
                try {
                    subscriber.onEvent().accept(event);
                } catch (RuntimeException e) {
                    log.debug("Falha ao entregar um aviso de tempo real", e);
                }
            });
        }
    }

    /** Encerra todas as assinaturas: cada tela fecha a conexão e reconecta sozinha. */
    private void endAll() {
        subscribers.values().forEach(set -> set.forEach(subscriber -> {
            try {
                subscriber.onEnd().run();
            } catch (RuntimeException e) {
                log.debug("Falha ao encerrar uma assinatura de tempo real", e);
            }
        }));
        subscribers.clear();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
