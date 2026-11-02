package br.com.cardapio.adapter.in.sse;

import br.com.cardapio.application.port.in.realtime.LiveEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * As conexões SSE abertas: cria cada uma, liga à assinatura dos eventos e cuida do fim dela.
 *
 * - Cada conexão dura no máximo 15 minutos. O navegador reconecta sozinho, e a reconexão passa
 *   de novo pela segurança: um garçom desligado ou uma sessão de mesa vencida param de receber.
 * - A cada 20 segundos vai um "ping" (comentário SSE, que a tela ignora), para proxies não
 *   fecharem a conexão parada e para descobrir telas que já sumiram.
 */
@Component
class SseStreams {

    static final Duration TIMEOUT = Duration.ofMinutes(15);
    static final Duration HEARTBEAT = Duration.ofSeconds(20);

    /** O que liga uma conexão aos eventos: recebe para onde mandar e devolve o que desliga. */
    interface Subscription {
        Runnable start(Consumer<LiveEvent> onEvent, Runnable onEnd);
    }

    private final Set<SseEmitter> open = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("sse-heartbeat").daemon().factory());

    SseStreams() {
        heartbeat.scheduleAtFixedRate(this::ping, HEARTBEAT.toSeconds(), HEARTBEAT.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Abre a conexão. Se a assinatura falhar (sessão inválida, por exemplo), a exceção sobe antes
     * de qualquer byte ir para a tela, e a resposta é o erro normal (401, 404).
     */
    SseEmitter open(Subscription subscription) {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        Runnable stop = subscription.start(event -> send(emitter, event), emitter::complete);
        open.add(emitter);
        Runnable cleanup = () -> {
            open.remove(emitter);
            stop.run();
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> cleanup.run());
        // O primeiro aviso confirma a conexão: a tela busca o estado atual a partir daqui.
        send(emitter, new LiveEvent("ready", Map.of()));
        return emitter;
    }

    private void send(SseEmitter emitter, LiveEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.name()).data(event.data(), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            emitter.complete(); // a tela foi embora; o onCompletion desfaz a assinatura
        }
    }

    private void ping() {
        for (SseEmitter emitter : open) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                emitter.complete();
            }
        }
    }

    @PreDestroy
    void close() {
        heartbeat.shutdownNow();
        open.forEach(SseEmitter::complete);
    }
}
