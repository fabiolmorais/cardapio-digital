package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.device.exception.DeviceNotFoundException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceAdminServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    private final DeviceFakes fakes = new DeviceFakes();
    private final ListDevicesService list = new ListDevicesService(fakes);
    private final UnlinkDeviceService unlink = new UnlinkDeviceService(fakes, fakes, fakes.clock());
    private final EstablishmentId sakura = EstablishmentId.newId();

    private Device given(EstablishmentId establishmentId, String name) {
        Device device = Device.link(establishmentId, name, DeviceType.WAITER, NOW);
        fakes.save(device);
        return device;
    }

    @Test
    void listsOnlyActiveDevicesOfThisRestaurant() {
        given(sakura, "TV do salão");
        Device phone = given(sakura, "Celular do salão");
        given(EstablishmentId.newId(), "Celular de outro restaurante");
        unlink.unlink(sakura, given(sakura, "Tablet antigo").id());

        var page = list.list(sakura, new PageQuery(0, 20));

        assertThat(page.content()).extracting(DeviceSummary::name).containsExactly("Celular do salão", "TV do salão");
        assertThat(page.content().getFirst().id()).isEqualTo(phone.id());
    }

    @Test
    void unlinkIsIdempotent() {
        Device phone = given(sakura, "Celular do salão");

        unlink.unlink(sakura, phone.id());
        unlink.unlink(sakura, phone.id());

        assertThat(fakes.devices.get(phone.id()).isActive()).isFalse();
    }

    @Test
    void unlinkEndsTheSessionsOpenedOnTheDevice() {
        Device phone = given(sakura, "Celular do salão");
        Device tablet = given(sakura, "Tablet da cozinha");
        RefreshTokenId onPhone = RefreshTokenId.newId();
        RefreshTokenId onTablet = RefreshTokenId.newId();
        fakes.register(onPhone, phone.id(), sakura, NOW);
        fakes.register(onTablet, tablet.id(), sakura, NOW);

        unlink.unlink(sakura, phone.id());

        assertThat(fakes.revokedSessions).containsExactly(onPhone);
    }

    @Test
    void deviceOfAnotherRestaurantIsNotFound() {
        Device elsewhere = given(EstablishmentId.newId(), "Celular de outro restaurante");

        assertThatThrownBy(() -> unlink.unlink(sakura, elsewhere.id())).isInstanceOf(DeviceNotFoundException.class);
        assertThat(fakes.devices.get(elsewhere.id()).isActive()).isTrue();
    }
}
