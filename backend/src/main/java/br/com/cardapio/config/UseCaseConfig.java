package br.com.cardapio.config;

import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.service.plan.ListPlansService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os casos de uso como beans. Os serviços de aplicação não usam @Service.
 */
@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

    @Bean
    ListPlansUseCase listPlansUseCase(LoadPlansPort loadPlansPort) {
        return new ListPlansService(loadPlansPort);
    }
}
