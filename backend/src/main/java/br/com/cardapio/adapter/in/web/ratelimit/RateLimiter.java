package br.com.cardapio.adapter.in.web.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conta os pedidos por chave (regra + IP) em janelas fixas: a primeira tentativa abre a janela,
 * e ela zera quando o tempo passa. Fica na memória de cada instância: com duas instâncias, o
 * limite efetivo dobra, o que ainda barra quem martela (a ideia é cortar a força bruta, não
 * contar cada pedido com exatidão).
 */
class RateLimiter {

    /** Acima disso, a próxima conta varre as janelas vencidas (IPs que não voltaram). */
    private static final int SWEEP_ABOVE = 10_000;

    private record Window(Instant start, Duration length, int count) {

        boolean expired(Instant now) {
            return !now.isBefore(start.plus(length));
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Conta mais um pedido. Vazio se passou; senão, quanto falta para a janela zerar. */
    Optional<Duration> tryAcquire(String key, RateLimitProperties.Limit limit) {
        Instant now = clock.instant();
        if (windows.size() > SWEEP_ABOVE) {
            windows.values().removeIf(w -> w.expired(now));
        }
        Window window = windows.compute(key, (k, w) -> w == null || w.expired(now)
                ? new Window(now, limit.window(), 1)
                : new Window(w.start(), w.length(), w.count() + 1));
        if (window.count() <= limit.requests()) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, window.start().plus(window.length())));
    }

    int size() {
        return windows.size();
    }
}
