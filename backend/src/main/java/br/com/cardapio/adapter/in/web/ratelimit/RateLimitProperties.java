package br.com.cardapio.adapter.in.web.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Os limites por IP (app.rate-limit). Uma rede com o mesmo IP pode ser o restaurante inteiro (o
 * Wi-Fi do salão, que a trava de presença exige): os limites da mesa são folgados de propósito.
 *
 * @param enabled false desliga tudo (os testes ponta a ponta fazem centenas de logins do mesmo IP)
 */
@ConfigurationProperties("app.rate-limit")
record RateLimitProperties(boolean enabled, Map<RateLimitRule, Limit> rules) {

    /** Até {@code requests} pedidos a cada {@code window}. */
    record Limit(int requests, Duration window) {

        Limit {
            Objects.requireNonNull(window, "informe a janela do limite");
            if (requests < 1 || window.isNegative() || window.isZero()) {
                throw new IllegalArgumentException("limite por IP inválido");
            }
        }
    }

    RateLimitProperties {
        rules = rules == null ? Map.of() : new EnumMap<>(rules);
        if (enabled) {
            for (RateLimitRule rule : RateLimitRule.values()) {
                if (!rules.containsKey(rule)) {
                    throw new IllegalArgumentException("falta o limite de " + rule + " em app.rate-limit.rules");
                }
            }
        }
    }
}
