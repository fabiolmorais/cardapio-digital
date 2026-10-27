package br.com.cardapio.application.port.out.device;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;

import java.util.Optional;

public interface DevicePort {

    /** Grava um aparelho novo ou as mudanças de um que já existe. */
    void save(Device device);

    /** Aparelhos ativos do restaurante, por nome. */
    ResultPage<Device> findActive(EstablishmentId establishmentId, PageQuery page);

    /** Vazio se o aparelho não existe ou é de outro restaurante. */
    Optional<Device> find(EstablishmentId establishmentId, DeviceId id);

    /**
     * Busca sem restaurante, para o próprio aparelho buscar o token. Só é usada depois de
     * conferir o segredo do pedido de vínculo, que é o que prova quem está pedindo.
     */
    Optional<LinkedDevice> findForClaim(DeviceId id);
}
