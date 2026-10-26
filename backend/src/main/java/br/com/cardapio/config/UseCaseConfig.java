package br.com.cardapio.config;

import br.com.cardapio.application.port.in.account.GetCurrentAccountUseCase;
import br.com.cardapio.application.port.in.account.LoginUseCase;
import br.com.cardapio.application.port.in.account.SignupUseCase;
import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.account.SaveAccountPort;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.service.account.GetCurrentAccountService;
import br.com.cardapio.application.service.account.LoginService;
import br.com.cardapio.application.service.account.SignupService;
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

    @Bean
    SignupUseCase signupUseCase(LoadUserPort loadUserPort, LoadEstablishmentPort loadEstablishmentPort,
                                SaveAccountPort saveAccountPort, PasswordHasher passwordHasher) {
        return new SignupService(loadUserPort, loadEstablishmentPort, saveAccountPort, passwordHasher);
    }

    @Bean
    LoginUseCase loginUseCase(LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort,
                              PasswordHasher passwordHasher, AccessTokenIssuer accessTokenIssuer) {
        return new LoginService(loadUserPort, loadMembershipPort, passwordHasher, accessTokenIssuer);
    }

    @Bean
    GetCurrentAccountUseCase getCurrentAccountUseCase(LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort) {
        return new GetCurrentAccountService(loadUserPort, loadMembershipPort);
    }
}
