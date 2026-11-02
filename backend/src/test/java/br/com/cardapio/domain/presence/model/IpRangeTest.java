package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpRangeTest {

    @Test
    void aSingleIpv4MatchesOnlyItself() {
        IpRange range = IpRange.parse("189.45.212.18");

        assertThat(range).hasToString("189.45.212.18/32");
        assertThat(range.contains("189.45.212.18")).isTrue();
        assertThat(range.contains("189.45.212.19")).isFalse();
    }

    @Test
    void theRangeIsKeptAsItsNetwork() {
        assertThat(IpRange.parse("189.45.212.7/22")).hasToString("189.45.212.0/22")
                .isEqualTo(IpRange.parse("189.45.212.0/22"));
    }

    @Test
    void aRangeComparesOnlyTheFirstBits() {
        IpRange range = IpRange.parse("189.45.212.0/22"); // 189.45.212.0 a 189.45.215.255

        assertThat(range.contains("189.45.215.200")).isTrue();
        assertThat(range.contains("189.45.216.1")).isFalse();
    }

    @Test
    void ipv6UsesTheNetworkPart() {
        IpRange range = IpRange.networkOf("2804:14c:6a:1:a1b2:c3d4:e5f6:789");

        assertThat(range).hasToString("2804:14c:6a:1::/64");
        // O fim do endereço muda (outro celular, ou o mesmo depois de um tempo); a rede é a mesma.
        assertThat(range.contains("2804:14c:6a:1:ffff:1:2:3")).isTrue();
        assertThat(range.contains("2804:14c:6a:2::1")).isFalse();
    }

    @Test
    void ipv4AndIpv6NeverMix() {
        assertThat(IpRange.parse("189.45.212.18").contains("::ffff:1")).isFalse();
        assertThat(IpRange.parse("2804:14c:6a::/48").contains("189.45.212.18")).isFalse();
        assertThat(IpRange.parse("189.45.212.18").contains("não é um ip")).isFalse();
    }

    @Test
    void namesAndNonsenseAreRefusedWithoutAnyDnsLookup() {
        for (String text : new String[]{"restaurante.com.br", "189.45.212.18/40", "189.45.212.18/4", "1.2.3/24/1",
                "", " ", "189.45.212.18/x"}) {
            assertThatThrownBy(() -> IpRange.parse(text)).as(text).isInstanceOf(InvalidValueException.class);
        }
    }

    @Test
    void ipv6IsWrittenInTheShortForm() {
        assertThat(IpRange.parse("2804:14c:6a:0:0:0:0:0/48")).hasToString("2804:14c:6a::/48");
        assertThat(IpRange.parse("::1")).hasToString("::1/128");
        assertThat(IpRange.parse("2001:db8:0:1:0:0:0:1")).hasToString("2001:db8:0:1::1/128");
        // Com duas sequências do mesmo tamanho, a primeira vira "::"; um zero sozinho fica.
        assertThat(IpRange.parse("2001:0:0:1:0:0:1:1")).hasToString("2001::1:0:0:1:1/128");
        assertThat(IpRange.parse("2001:db8:0:1:1:1:1:1")).hasToString("2001:db8:0:1:1:1:1:1/128");
    }
}
