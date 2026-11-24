package br.com.cardapio.domain.report.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * O uso do celular nas mesas (REL-64, 65, 67, 68, 69).
 *
 * @param phonesPerTab     celulares por comanda (das comandas com celular); vazio sem elas
 * @param orderingPerTab   desses, quantos pediram, por comanda
 * @param scanToOrder      do QR ao primeiro pedido do celular, em média; vazio sem pedidos
 * @param scannedNoOrder   mesas (mesa e dia) em que alguém escaneou e ninguém pediu nada
 * @param rejected         pedidos do celular recusados pelo garçom
 * @param blocked          celulares bloqueados pela equipe
 */
public record PhoneStats(Optional<Double> phonesPerTab, Optional<Double> orderingPerTab,
                         Optional<Duration> scanToOrder, int scannedNoOrder, int rejected, int blocked) {

    public PhoneStats {
        Objects.requireNonNull(phonesPerTab, "celulares por comanda é obrigatório");
        Objects.requireNonNull(orderingPerTab, "celulares que pedem é obrigatório");
        Objects.requireNonNull(scanToOrder, "tempo até o pedido é obrigatório");
    }
}
