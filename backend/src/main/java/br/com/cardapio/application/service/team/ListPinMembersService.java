package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.ListPinMembersUseCase;
import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import br.com.cardapio.domain.device.exception.PinNotAllowedOnDeviceException;
import org.springframework.transaction.annotation.Transactional;

public class ListPinMembersService implements ListPinMembersUseCase {

    private final DeviceAuthenticator authenticator;
    private final TeamMemberPort team;

    public ListPinMembersService(DeviceAuthenticator authenticator, TeamMemberPort team) {
        this.authenticator = authenticator;
        this.team = team;
    }

    @Override
    @Transactional
    public ResultPage<TeamMemberSummary> list(String deviceToken, PageQuery page) {
        LinkedDevice linked = authenticator.authenticate(deviceToken);
        if (!linked.device().acceptsPinLogin()) {
            throw new PinNotAllowedOnDeviceException();
        }
        // O restaurante vem do aparelho: o token decide de quem é a lista.
        return team.findPinMembers(linked.device().establishmentId(), page).map(TeamMembers::summary);
    }
}
