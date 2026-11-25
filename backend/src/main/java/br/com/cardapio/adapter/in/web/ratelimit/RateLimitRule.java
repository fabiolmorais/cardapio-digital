package br.com.cardapio.adapter.in.web.ratelimit;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * As rotas com limite de tentativas por IP, em grupos: cada grupo conta à parte. Só as rotas
 * públicas (ou de entrada) que alguém de fora poderia martelar; o resto já exige login.
 */
enum RateLimitRule {
    /** Entrar com e-mail e senha. */
    LOGIN(Set.of("POST"), "/api/v1/auth/login"),
    /** Trocar de unidade (já exige login; o limite só segura um script em volta). */
    SWITCH(Set.of("POST"), "/api/v1/auth/switch"),
    /** Criar uma conta (o teste grátis). */
    SIGNUP(Set.of("POST"), "/api/v1/signup"),
    /** "Esqueci a senha": pedir o link e trocar a senha com ele. */
    PASSWORD_RESET(Set.of("POST"), "/api/v1/auth/password-reset(/confirm)?"),
    /** Entrar com PIN num aparelho vinculado. */
    PIN(Set.of("POST"), "/api/v1/auth/pin"),
    /** Um aparelho pedindo o código de vínculo. */
    PAIRING(Set.of("POST"), "/api/v1/device-pairings"),
    /** Um celular abrindo a sessão da mesa (cada QR escaneado). */
    TABLE_SESSION(Set.of("POST"), "/api/v1/public/tables/[^/]+/session"),
    /** O que a mesa faz: chamar, pedir, provar a presença, virar o cartão, cancelar, avaliar. */
    TABLE_ACTIONS(Set.of("POST", "PUT", "DELETE"),
            "/api/v1/public/tables/[^/]+/(calls|orders|rounds|presence|rodizio/card|order-items|rating)(/[^/]+)?"),
    /**
     * Os eventos do cardápio (prato aberto, reel visto, carrinho): à parte das ações, com um limite
     * folgado, porque todos os celulares do salão saem pelo mesmo IP do Wi-Fi e mandam em lote.
     */
    TABLE_EVENTS(Set.of("POST"), "/api/v1/public/tables/[^/]+/events");

    private final Set<String> methods;
    private final Pattern path;

    RateLimitRule(Set<String> methods, String path) {
        this.methods = methods;
        this.path = Pattern.compile(path);
    }

    /** A regra desta requisição, se houver. */
    static Optional<RateLimitRule> of(String method, String path) {
        for (RateLimitRule rule : values()) {
            if (rule.methods.contains(method) && rule.path.matcher(path).matches()) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }
}
