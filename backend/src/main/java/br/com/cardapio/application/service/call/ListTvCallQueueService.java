package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.ListTvCallQueueUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import org.springframework.transaction.annotation.Transactional;

public class ListTvCallQueueService implements ListTvCallQueueUseCase {

    private final DeviceAuthenticator devices;
    private final CallPort calls;

    public ListTvCallQueueService(DeviceAuthenticator devices, CallPort calls) {
        this.devices = devices;
        this.calls = calls;
    }

    /**
     * O restaurante vem do aparelho (do token do cookie), nunca de um parâmetro: é a mesma regra
     * do login. Não é readOnly: o aparelho fica marcado como "visto agora".
     */
    @Override
    @Transactional
    public ResultPage<StaffCall> list(String deviceToken, PageQuery page) {
        var establishmentId = devices.authenticateTv(deviceToken).device().establishmentId();
        return calls.findQueue(establishmentId, page).map(StaffCall::from);
    }
}
