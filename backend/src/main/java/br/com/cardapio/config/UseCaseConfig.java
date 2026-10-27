package br.com.cardapio.config;

import br.com.cardapio.application.port.in.account.*;
import br.com.cardapio.application.port.in.device.ClaimPairingUseCase;
import br.com.cardapio.application.port.in.device.LinkDeviceUseCase;
import br.com.cardapio.application.port.in.device.ListDevicesUseCase;
import br.com.cardapio.application.port.in.device.StartPairingUseCase;
import br.com.cardapio.application.port.in.device.UnlinkDeviceUseCase;
import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.application.port.in.team.AddTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.DeactivateTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.ListTeamUseCase;
import br.com.cardapio.application.port.in.team.ReactivateTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.UpdateTeamMemberUseCase;
import br.com.cardapio.application.port.out.account.*;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.service.account.*;
import br.com.cardapio.application.service.device.ClaimPairingService;
import br.com.cardapio.application.service.device.LinkDeviceService;
import br.com.cardapio.application.service.device.ListDevicesService;
import br.com.cardapio.application.service.device.StartPairingService;
import br.com.cardapio.application.service.device.UnlinkDeviceService;
import br.com.cardapio.application.service.plan.ListPlansService;
import br.com.cardapio.application.service.team.AddTeamMemberService;
import br.com.cardapio.application.service.team.DeactivateTeamMemberService;
import br.com.cardapio.application.service.team.ListTeamService;
import br.com.cardapio.application.service.team.ReactivateTeamMemberService;
import br.com.cardapio.application.service.team.UpdateTeamMemberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

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

    /** Não é caso de uso: é a peça que login e renovação usam para montar a sessão. */
    @Bean
    SessionIssuer sessionIssuer(AccessTokenIssuer accessTokenIssuer, RefreshTokenPort refreshTokenPort,
                                RandomTokenGenerator randomTokenGenerator, Clock clock,
                                @Value("${app.security.refresh-token.ttl}") Duration refreshTokenTtl) {
        return new SessionIssuer(accessTokenIssuer, refreshTokenPort, randomTokenGenerator, clock, refreshTokenTtl);
    }

    @Bean
    LoginUseCase loginUseCase(LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort,
                              PasswordHasher passwordHasher, SessionIssuer sessionIssuer) {
        return new LoginService(loadUserPort, loadMembershipPort, passwordHasher, sessionIssuer);
    }

    @Bean
    RefreshSessionUseCase refreshSessionUseCase(RefreshTokenPort refreshTokenPort, LoadUserPort loadUserPort,
                                                LoadMembershipPort loadMembershipPort, SessionIssuer sessionIssuer,
                                                Clock clock) {
        return new RefreshSessionService(refreshTokenPort, loadUserPort, loadMembershipPort, sessionIssuer, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(RefreshTokenPort refreshTokenPort, Clock clock) {
        return new LogoutService(refreshTokenPort, clock);
    }

    @Bean
    GetCurrentAccountUseCase getCurrentAccountUseCase(LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort) {
        return new GetCurrentAccountService(loadUserPort, loadMembershipPort);
    }

    @Bean
    ListTeamUseCase listTeamUseCase(TeamMemberPort teamMemberPort) {
        return new ListTeamService(teamMemberPort);
    }

    @Bean
    AddTeamMemberUseCase addTeamMemberUseCase(TeamMemberPort teamMemberPort, PasswordHasher passwordHasher) {
        return new AddTeamMemberService(teamMemberPort, passwordHasher);
    }

    @Bean
    UpdateTeamMemberUseCase updateTeamMemberUseCase(TeamMemberPort teamMemberPort, PasswordHasher passwordHasher) {
        return new UpdateTeamMemberService(teamMemberPort, passwordHasher);
    }

    @Bean
    DeactivateTeamMemberUseCase deactivateTeamMemberUseCase(TeamMemberPort teamMemberPort) {
        return new DeactivateTeamMemberService(teamMemberPort);
    }

    @Bean
    ReactivateTeamMemberUseCase reactivateTeamMemberUseCase(TeamMemberPort teamMemberPort) {
        return new ReactivateTeamMemberService(teamMemberPort);
    }

    @Bean
    StartPairingUseCase startPairingUseCase(DevicePairingPort devicePairingPort, RandomCodeGenerator randomCodeGenerator,
                                            RandomTokenGenerator randomTokenGenerator, Clock clock,
                                            @Value("${app.devices.pairing-ttl}") Duration pairingTtl) {
        return new StartPairingService(devicePairingPort, randomCodeGenerator, randomTokenGenerator, clock, pairingTtl);
    }

    @Bean
    LinkDeviceUseCase linkDeviceUseCase(DevicePairingPort devicePairingPort, DevicePort devicePort, Clock clock) {
        return new LinkDeviceService(devicePairingPort, devicePort, clock);
    }

    @Bean
    ClaimPairingUseCase claimPairingUseCase(DevicePairingPort devicePairingPort, DevicePort devicePort,
                                            RandomTokenGenerator randomTokenGenerator, Clock clock) {
        return new ClaimPairingService(devicePairingPort, devicePort, randomTokenGenerator, clock);
    }

    @Bean
    ListDevicesUseCase listDevicesUseCase(DevicePort devicePort) {
        return new ListDevicesService(devicePort);
    }

    @Bean
    UnlinkDeviceUseCase unlinkDeviceUseCase(DevicePort devicePort, Clock clock) {
        return new UnlinkDeviceService(devicePort, clock);
    }
}
