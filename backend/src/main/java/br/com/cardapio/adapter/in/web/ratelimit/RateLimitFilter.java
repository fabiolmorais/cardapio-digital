package br.com.cardapio.adapter.in.web.ratelimit;

import br.com.cardapio.adapter.in.web.shared.ClientIp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * O limite de tentativas por IP (CLAUDE.md, "Segurança"): login, PIN, vínculo de aparelho e as
 * ações da mesa. Passou do limite, 429 com Retry-After e a frase em Problem Details.
 *
 * <p>Roda depois da cadeia do Spring Security (a ordem padrão de um filtro), e não antes: assim a
 * resposta 429 já sai com os cabeçalhos de CORS, e a tela consegue ler a frase.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties properties;
    private final ClientIp clientIp;
    private final JsonMapper json;
    private final RateLimiter limiter;

    RateLimitFilter(RateLimitProperties properties, ClientIp clientIp, JsonMapper json, Clock clock) {
        this.properties = properties;
        this.clientIp = clientIp;
        this.json = json;
        this.limiter = new RateLimiter(clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var rule = RateLimitRule.of(request.getMethod(), request.getRequestURI());
        String ip = clientIp.of(request);
        if (rule.isPresent() && ip != null) {
            var retryAfter = limiter.tryAcquire(rule.get() + "|" + ip, properties.rules().get(rule.get()));
            if (retryAfter.isPresent()) {
                // Sem o IP no log: é dado pessoal (LGPD). A regra basta para notar um ataque.
                log.warn("Limite por IP atingido: {}", rule.get());
                tooMany(response, retryAfter.get());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void tooMany(HttpServletResponse response, Duration retryAfter) throws IOException {
        long seconds = Math.max(1, retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0));
        long minutes = (seconds + 59) / 60;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas desta rede. Espere " + (minutes == 1 ? "1 minuto" : minutes + " minutos")
                        + " e tente de novo.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(problem));
    }
}
