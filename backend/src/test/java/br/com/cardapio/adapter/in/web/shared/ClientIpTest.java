package br.com.cardapio.adapter.in.web.shared;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** De onde vem o IP do cliente: a trava de presença depende de ninguém conseguir inventá-lo. */
class ClientIpTest {

    private static MockHttpServletRequest fromOutside() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5"); // o proxy da hospedagem, por onde todo mundo chega
        request.addHeader("X-Forwarded-For", "189.45.212.18"); // escrito pelo próprio celular: não vale
        request.addHeader("Fly-Client-IP", "177.10.20.30"); // escrito só pelo proxy
        return request;
    }

    @Test
    void inProductionTheIpComesOnlyFromTheProxyHeader() {
        assertThat(new ClientIp("Fly-Client-IP").of(fromOutside())).isEqualTo("177.10.20.30");
    }

    @Test
    void withoutTheHeaderConfiguredItIsTheConnectionAddress() {
        assertThat(new ClientIp("").of(fromOutside())).isEqualTo("10.0.0.5");
    }

    @Test
    void aMissingProxyHeaderMeansNoIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        assertThat(new ClientIp("Fly-Client-IP").of(request)).isNull();
    }
}
