package br.com.cardapio.config;

import br.com.cardapio.application.port.in.account.*;
import br.com.cardapio.application.port.in.brand.GetBrandUseCase;
import br.com.cardapio.application.port.in.brand.RemoveLogoUseCase;
import br.com.cardapio.application.port.in.brand.SetLogoUseCase;
import br.com.cardapio.application.port.in.brand.StartLogoUploadUseCase;
import br.com.cardapio.application.port.in.brand.UpdateBrandUseCase;
import br.com.cardapio.application.port.in.call.AcceptCallUseCase;
import br.com.cardapio.application.port.in.call.BlockCallerUseCase;
import br.com.cardapio.application.port.in.call.CancelCallUseCase;
import br.com.cardapio.application.port.in.call.DeclineCallUseCase;
import br.com.cardapio.application.port.in.call.FinishCallUseCase;
import br.com.cardapio.application.port.in.call.ListCallQueueUseCase;
import br.com.cardapio.application.port.in.call.ListTableCallsUseCase;
import br.com.cardapio.application.port.in.call.ListTvCallQueueUseCase;
import br.com.cardapio.application.port.in.call.RequestCallUseCase;
import br.com.cardapio.application.port.in.device.ClaimPairingUseCase;
import br.com.cardapio.application.port.in.device.CurrentDeviceUseCase;
import br.com.cardapio.application.port.in.device.LinkDeviceUseCase;
import br.com.cardapio.application.port.in.device.ListDevicesUseCase;
import br.com.cardapio.application.port.in.device.StartPairingUseCase;
import br.com.cardapio.application.port.in.device.UnlinkDeviceUseCase;
import br.com.cardapio.application.port.in.floor.CreateSectorUseCase;
import br.com.cardapio.application.port.in.floor.CreateTableUseCase;
import br.com.cardapio.application.port.in.floor.DeactivateSectorUseCase;
import br.com.cardapio.application.port.in.floor.ListSectorsUseCase;
import br.com.cardapio.application.port.in.floor.ListTablesUseCase;
import br.com.cardapio.application.port.in.floor.RenameSectorUseCase;
import br.com.cardapio.application.port.in.floor.RotateTableTokenUseCase;
import br.com.cardapio.application.port.in.floor.UpdateTableUseCase;
import br.com.cardapio.application.port.in.floor.ViewPublicTableUseCase;
import br.com.cardapio.application.port.in.menu.*;
import br.com.cardapio.application.port.in.menu.AddProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.GetPublicMenuUseCase;
import br.com.cardapio.application.port.in.menu.ListProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.RemoveProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.ReorderProductMediaUseCase;
import br.com.cardapio.application.port.in.menu.StartMediaUploadUseCase;
import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.application.port.in.presence.CheckPresenceUseCase;
import br.com.cardapio.application.port.in.presence.GetPresenceRuleUseCase;
import br.com.cardapio.application.port.in.presence.UpdatePresenceRuleUseCase;
import br.com.cardapio.application.port.in.realtime.WatchStaffEventsUseCase;
import br.com.cardapio.application.port.in.realtime.WatchTableEventsUseCase;
import br.com.cardapio.application.port.in.realtime.WatchTvEventsUseCase;
import br.com.cardapio.application.port.in.tablesession.EndExpiredSessionsUseCase;
import br.com.cardapio.application.port.in.tablesession.StartTableSessionUseCase;
import br.com.cardapio.application.port.in.team.AddTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.DeactivateTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.ListPinMembersUseCase;
import br.com.cardapio.application.port.in.team.ListTeamUseCase;
import br.com.cardapio.application.port.in.team.ReactivateTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.UpdateTeamMemberUseCase;
import br.com.cardapio.application.port.out.account.*;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.DeviceSessionPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.floor.TableDisplayRenderer;
import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.service.account.*;
import br.com.cardapio.application.service.brand.GetBrandService;
import br.com.cardapio.application.service.brand.RemoveLogoService;
import br.com.cardapio.application.service.brand.SetLogoService;
import br.com.cardapio.application.service.brand.StartLogoUploadService;
import br.com.cardapio.application.service.brand.UpdateBrandService;
import br.com.cardapio.application.service.call.AcceptCallService;
import br.com.cardapio.application.service.call.BlockCallerService;
import br.com.cardapio.application.service.call.CancelCallService;
import br.com.cardapio.application.service.call.DeclineCallService;
import br.com.cardapio.application.service.call.FinishCallService;
import br.com.cardapio.application.service.call.ListCallQueueService;
import br.com.cardapio.application.service.call.ListTableCallsService;
import br.com.cardapio.application.service.call.ListTvCallQueueService;
import br.com.cardapio.application.service.call.RequestCallService;
import br.com.cardapio.application.service.device.ClaimPairingService;
import br.com.cardapio.application.service.device.CurrentDeviceService;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import br.com.cardapio.application.service.device.LinkDeviceService;
import br.com.cardapio.application.service.device.ListDevicesService;
import br.com.cardapio.application.service.device.StartPairingService;
import br.com.cardapio.application.service.device.UnlinkDeviceService;
import br.com.cardapio.application.service.floor.CreateSectorService;
import br.com.cardapio.application.service.floor.CreateTableService;
import br.com.cardapio.application.service.floor.DeactivateSectorService;
import br.com.cardapio.application.service.floor.ListSectorsService;
import br.com.cardapio.application.service.floor.ListTablesService;
import br.com.cardapio.application.service.floor.RenameSectorService;
import br.com.cardapio.application.service.floor.RotateTableTokenService;
import br.com.cardapio.application.service.floor.TableDisplayService;
import br.com.cardapio.application.service.floor.UpdateTableService;
import br.com.cardapio.application.service.floor.ViewPublicTableService;
import br.com.cardapio.application.service.menu.*;
import br.com.cardapio.application.service.menu.AddProductMediaService;
import br.com.cardapio.application.service.menu.GetPublicMenuService;
import br.com.cardapio.application.service.menu.ListProductMediaService;
import br.com.cardapio.application.service.menu.RemoveProductMediaService;
import br.com.cardapio.application.service.menu.ReorderProductMediaService;
import br.com.cardapio.application.service.menu.StartMediaUploadService;
import br.com.cardapio.application.service.plan.ListPlansService;
import br.com.cardapio.application.service.presence.CheckPresenceService;
import br.com.cardapio.application.service.presence.GetPresenceRuleService;
import br.com.cardapio.application.service.presence.UpdatePresenceRuleService;
import br.com.cardapio.application.service.realtime.WatchStaffEventsService;
import br.com.cardapio.application.service.realtime.WatchTableEventsService;
import br.com.cardapio.application.service.realtime.WatchTvEventsService;
import br.com.cardapio.application.service.tablesession.EndExpiredSessionsService;
import br.com.cardapio.application.service.tablesession.StartTableSessionService;
import br.com.cardapio.application.service.team.AddTeamMemberService;
import br.com.cardapio.application.service.team.DeactivateTeamMemberService;
import br.com.cardapio.application.service.team.ListPinMembersService;
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
    UnlinkDeviceUseCase unlinkDeviceUseCase(DevicePort devicePort, DeviceSessionPort deviceSessionPort, Clock clock) {
        return new UnlinkDeviceService(devicePort, deviceSessionPort, clock);
    }

    /** Não é caso de uso: é a peça que identifica o aparelho pelo token do cookie. */
    @Bean
    DeviceAuthenticator deviceAuthenticator(DevicePort devicePort, Clock clock) {
        return new DeviceAuthenticator(devicePort, clock);
    }

    @Bean
    CurrentDeviceUseCase currentDeviceUseCase(DeviceAuthenticator deviceAuthenticator) {
        return new CurrentDeviceService(deviceAuthenticator);
    }

    @Bean
    ListPinMembersUseCase listPinMembersUseCase(DeviceAuthenticator deviceAuthenticator, TeamMemberPort teamMemberPort) {
        return new ListPinMembersService(deviceAuthenticator, teamMemberPort);
    }

    @Bean
    PinLoginUseCase pinLoginUseCase(DeviceAuthenticator deviceAuthenticator, TeamMemberPort teamMemberPort,
                                    PinLockoutPort pinLockoutPort, PasswordHasher passwordHasher,
                                    LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort,
                                    SessionIssuer sessionIssuer, DeviceSessionPort deviceSessionPort, Clock clock) {
        return new PinLoginService(deviceAuthenticator, teamMemberPort, pinLockoutPort, passwordHasher, loadUserPort,
                loadMembershipPort, sessionIssuer, deviceSessionPort, clock);
    }

    @Bean
    ListSectorsUseCase listSectorsUseCase(SectorPort sectorPort) {
        return new ListSectorsService(sectorPort);
    }

    @Bean
    CreateSectorUseCase createSectorUseCase(SectorPort sectorPort) {
        return new CreateSectorService(sectorPort);
    }

    @Bean
    RenameSectorUseCase renameSectorUseCase(SectorPort sectorPort) {
        return new RenameSectorService(sectorPort);
    }

    @Bean
    DeactivateSectorUseCase deactivateSectorUseCase(SectorPort sectorPort) {
        return new DeactivateSectorService(sectorPort);
    }

    @Bean
    ListTablesUseCase listTablesUseCase(DiningTablePort diningTablePort) {
        return new ListTablesService(diningTablePort);
    }

    @Bean
    CreateTableUseCase createTableUseCase(SectorPort sectorPort, DiningTablePort diningTablePort,
                                          RandomTokenGenerator randomTokenGenerator, Clock clock) {
        return new CreateTableService(sectorPort, diningTablePort, randomTokenGenerator, clock);
    }

    @Bean
    UpdateTableUseCase updateTableUseCase(SectorPort sectorPort, DiningTablePort diningTablePort) {
        return new UpdateTableService(sectorPort, diningTablePort);
    }

    @Bean
    RotateTableTokenUseCase rotateTableTokenUseCase(DiningTablePort diningTablePort,
                                                    RandomTokenGenerator randomTokenGenerator, Clock clock) {
        return new RotateTableTokenService(diningTablePort, randomTokenGenerator, clock);
    }

    /**
     * Um bean só atende aos dois casos de uso de impressão: o TableDisplayService implementa as
     * duas portas, e o Spring o injeta onde pedirem qualquer uma delas.
     */
    @Bean
    TableDisplayService tableDisplayService(DiningTablePort diningTablePort, SectorPort sectorPort,
                                            BrandPort brandPort, MediaStoragePort mediaStoragePort,
                                            TableDisplayRenderer tableDisplayRenderer,
                                            @Value("${app.public-url}") String publicUrl) {
        return new TableDisplayService(diningTablePort, sectorPort, brandPort, mediaStoragePort, tableDisplayRenderer,
                publicUrl);
    }

    @Bean
    ViewPublicTableUseCase viewPublicTableUseCase(DiningTablePort diningTablePort, SectorPort sectorPort,
                                                  BrandPort brandPort, MediaStoragePort mediaStoragePort) {
        return new ViewPublicTableService(diningTablePort, sectorPort, brandPort, mediaStoragePort);
    }

    // Cardápio (Guia 9)

    @Bean
    ListMenuCategoriesUseCase listMenuCategoriesUseCase(MenuCategoryPort menuCategoryPort) {
        return new ListMenuCategoriesService(menuCategoryPort);
    }

    @Bean
    CreateMenuCategoryUseCase createMenuCategoryUseCase(MenuCategoryPort menuCategoryPort) {
        return new CreateMenuCategoryService(menuCategoryPort);
    }

    @Bean
    RenameMenuCategoryUseCase renameMenuCategoryUseCase(MenuCategoryPort menuCategoryPort) {
        return new RenameMenuCategoryService(menuCategoryPort);
    }

    @Bean
    DeactivateMenuCategoryUseCase deactivateMenuCategoryUseCase(MenuCategoryPort menuCategoryPort) {
        return new DeactivateMenuCategoryService(menuCategoryPort);
    }

    @Bean
    ReorderMenuCategoriesUseCase reorderMenuCategoriesUseCase(MenuCategoryPort menuCategoryPort) {
        return new ReorderMenuCategoriesService(menuCategoryPort);
    }

    @Bean
    ListProductsUseCase listProductsUseCase(ProductPort productPort) {
        return new ListProductsService(productPort);
    }

    @Bean
    GetProductUseCase getProductUseCase(ProductPort productPort) {
        return new GetProductService(productPort);
    }

    @Bean
    CreateProductUseCase createProductUseCase(MenuCategoryPort menuCategoryPort, ProductPort productPort) {
        return new CreateProductService(menuCategoryPort, productPort);
    }

    @Bean
    UpdateProductUseCase updateProductUseCase(MenuCategoryPort menuCategoryPort, ProductPort productPort) {
        return new UpdateProductService(menuCategoryPort, productPort);
    }

    @Bean
    ChangeProductAvailabilityUseCase changeProductAvailabilityUseCase(ProductPort productPort) {
        return new ChangeProductAvailabilityService(productPort);
    }

    @Bean
    DeactivateProductUseCase deactivateProductUseCase(ProductPort productPort) {
        return new DeactivateProductService(productPort);
    }

    @Bean
    ReorderProductsUseCase reorderProductsUseCase(MenuCategoryPort menuCategoryPort, ProductPort productPort) {
        return new ReorderProductsService(menuCategoryPort, productPort);
    }

    // Fotos dos produtos (Guia 10)

    @Bean
    StartMediaUploadUseCase startMediaUploadUseCase(ProductPort productPort, ProductMediaPort productMediaPort,
                                                    MediaStoragePort mediaStoragePort) {
        return new StartMediaUploadService(productPort, productMediaPort, mediaStoragePort);
    }

    @Bean
    AddProductMediaUseCase addProductMediaUseCase(ProductPort productPort, ProductMediaPort productMediaPort,
                                                  MediaStoragePort mediaStoragePort,
                                                  ImageProcessorPort imageProcessorPort) {
        return new AddProductMediaService(productPort, productMediaPort, mediaStoragePort, imageProcessorPort);
    }

    @Bean
    ListProductMediaUseCase listProductMediaUseCase(ProductPort productPort, ProductMediaPort productMediaPort,
                                                    MediaStoragePort mediaStoragePort) {
        return new ListProductMediaService(productPort, productMediaPort, mediaStoragePort);
    }

    @Bean
    RemoveProductMediaUseCase removeProductMediaUseCase(ProductMediaPort productMediaPort) {
        return new RemoveProductMediaService(productMediaPort);
    }

    @Bean
    ReorderProductMediaUseCase reorderProductMediaUseCase(ProductPort productPort, ProductMediaPort productMediaPort) {
        return new ReorderProductMediaService(productPort, productMediaPort);
    }

    // Marca e aparência (Guia 11)

    @Bean
    GetBrandUseCase getBrandUseCase(BrandPort brandPort, MediaStoragePort mediaStoragePort) {
        return new GetBrandService(brandPort, mediaStoragePort);
    }

    @Bean
    UpdateBrandUseCase updateBrandUseCase(BrandPort brandPort, MediaStoragePort mediaStoragePort) {
        return new UpdateBrandService(brandPort, mediaStoragePort);
    }

    @Bean
    StartLogoUploadUseCase startLogoUploadUseCase(MediaStoragePort mediaStoragePort) {
        return new StartLogoUploadService(mediaStoragePort);
    }

    @Bean
    SetLogoUseCase setLogoUseCase(BrandPort brandPort, MediaStoragePort mediaStoragePort,
                                  ImageProcessorPort imageProcessorPort) {
        return new SetLogoService(brandPort, mediaStoragePort, imageProcessorPort);
    }

    @Bean
    RemoveLogoUseCase removeLogoUseCase(BrandPort brandPort, MediaStoragePort mediaStoragePort) {
        return new RemoveLogoService(brandPort, mediaStoragePort);
    }

    // Página da mesa (Guia 12)

    @Bean
    GetPublicMenuUseCase getPublicMenuUseCase(DiningTablePort diningTablePort, MenuCategoryPort menuCategoryPort,
                                              ProductPort productPort, ProductMediaPort productMediaPort,
                                              MediaStoragePort mediaStoragePort) {
        return new GetPublicMenuService(diningTablePort, menuCategoryPort, productPort, productMediaPort,
                mediaStoragePort);
    }

    @Bean
    StartTableSessionUseCase startTableSessionUseCase(DiningTablePort diningTablePort,
                                                      TableSessionPort tableSessionPort,
                                                      PresenceRulePort presenceRulePort,
                                                      RandomTokenGenerator randomTokenGenerator, Clock clock) {
        return new StartTableSessionService(diningTablePort, tableSessionPort, presenceRulePort,
                randomTokenGenerator, clock);
    }

    // Chamados (Guia 13), que publicam os avisos de tempo real (Guia 14)

    @Bean
    RequestCallUseCase requestCallUseCase(DiningTablePort diningTablePort, TableSessionPort tableSessionPort,
                                          CallPort callPort, EventPublisherPort eventPublisherPort, Clock clock) {
        return new RequestCallService(diningTablePort, tableSessionPort, callPort, eventPublisherPort, clock);
    }

    @Bean
    ListTableCallsUseCase listTableCallsUseCase(DiningTablePort diningTablePort, TableSessionPort tableSessionPort,
                                                CallPort callPort, Clock clock) {
        return new ListTableCallsService(diningTablePort, tableSessionPort, callPort, clock);
    }

    @Bean
    CancelCallUseCase cancelCallUseCase(DiningTablePort diningTablePort, TableSessionPort tableSessionPort,
                                        CallPort callPort, EventPublisherPort eventPublisherPort, Clock clock) {
        return new CancelCallService(diningTablePort, tableSessionPort, callPort, eventPublisherPort, clock);
    }

    @Bean
    ListCallQueueUseCase listCallQueueUseCase(CallPort callPort) {
        return new ListCallQueueService(callPort);
    }

    @Bean
    AcceptCallUseCase acceptCallUseCase(CallPort callPort, TableSessionPort tableSessionPort,
                                        EventPublisherPort eventPublisherPort, Clock clock) {
        return new AcceptCallService(callPort, tableSessionPort, eventPublisherPort, clock);
    }

    @Bean
    FinishCallUseCase finishCallUseCase(CallPort callPort, EventPublisherPort eventPublisherPort, Clock clock) {
        return new FinishCallService(callPort, eventPublisherPort, clock);
    }

    // Tempo real (Guia 14)

    @Bean
    WatchStaffEventsUseCase watchStaffEventsUseCase(EventStreamPort eventStreamPort) {
        return new WatchStaffEventsService(eventStreamPort);
    }

    @Bean
    WatchTableEventsUseCase watchTableEventsUseCase(DiningTablePort diningTablePort,
                                                    TableSessionPort tableSessionPort,
                                                    EventStreamPort eventStreamPort, Clock clock) {
        return new WatchTableEventsService(diningTablePort, tableSessionPort, eventStreamPort, clock);
    }

    // Modo TV na TV vinculada (Guia 15)

    @Bean
    ListTvCallQueueUseCase listTvCallQueueUseCase(DeviceAuthenticator deviceAuthenticator, CallPort callPort) {
        return new ListTvCallQueueService(deviceAuthenticator, callPort);
    }

    @Bean
    WatchTvEventsUseCase watchTvEventsUseCase(DeviceAuthenticator deviceAuthenticator,
                                              EventStreamPort eventStreamPort) {
        return new WatchTvEventsService(deviceAuthenticator, eventStreamPort);
    }

    // Trava de presença (Guia 16)

    @Bean
    CheckPresenceUseCase checkPresenceUseCase(DiningTablePort diningTablePort, TableSessionPort tableSessionPort,
                                              PresenceRulePort presenceRulePort, Clock clock) {
        return new CheckPresenceService(diningTablePort, tableSessionPort, presenceRulePort, clock);
    }

    @Bean
    GetPresenceRuleUseCase getPresenceRuleUseCase(PresenceRulePort presenceRulePort, Clock clock) {
        return new GetPresenceRuleService(presenceRulePort, clock);
    }

    @Bean
    UpdatePresenceRuleUseCase updatePresenceRuleUseCase(PresenceRulePort presenceRulePort, Clock clock) {
        return new UpdatePresenceRuleService(presenceRulePort, clock);
    }

    @Bean
    DeclineCallUseCase declineCallUseCase(CallPort callPort, EventPublisherPort eventPublisherPort, Clock clock) {
        return new DeclineCallService(callPort, eventPublisherPort, clock);
    }

    // Fim da trava de presença (Guia 17)

    @Bean
    BlockCallerUseCase blockCallerUseCase(CallPort callPort, TableSessionPort tableSessionPort,
                                          EventPublisherPort eventPublisherPort, Clock clock) {
        return new BlockCallerService(callPort, tableSessionPort, eventPublisherPort, clock);
    }

    @Bean
    EndExpiredSessionsUseCase endExpiredSessionsUseCase(TableSessionPort tableSessionPort, Clock clock) {
        return new EndExpiredSessionsService(tableSessionPort, clock);
    }
}
