package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Uma faixa de IPs (CIDR): o endereço e quantos bits do começo precisam bater. "189.45.212.18/32"
 * é um IP só; "2804:14c:6a:1::/64" é uma rede IPv6 inteira (no IPv6, o celular troca o final do
 * endereço o tempo todo, mas os primeiros 64 bits são os da rede do restaurante).
 */
public record IpRange(InetAddress address, int prefixBits) {

    /** Só dígitos, letras de A a F, ":" e ".": assim o texto nunca é tratado como nome de servidor (DNS). */
    private static final Pattern LITERAL = Pattern.compile("[0-9a-fA-F:.]{2,45}");

    public IpRange {
        Objects.requireNonNull(address);
        int max = address.getAddress().length * 8;
        if (prefixBits < 8 || prefixBits > max) {
            throw new InvalidValueException("Faixa de IP inválida: o tamanho deve ser de 8 a " + max);
        }
        address = networkAddress(address, prefixBits);
    }

    /** Zera os bits depois do prefixo: "189.45.212.7/22" e "189.45.212.0/22" são a mesma faixa. */
    private static InetAddress networkAddress(InetAddress address, int prefixBits) {
        byte[] bytes = address.getAddress();
        for (int bit = prefixBits; bit < bytes.length * 8; bit++) {
            bytes[bit / 8] &= (byte) ~(0x80 >>> (bit % 8));
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e); // não acontece: o tamanho veio de um endereço válido
        }
    }

    /** "189.45.212.18" (um IP) ou "2804:14c:6a::/48" (uma faixa). */
    public static IpRange parse(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidValueException("Informe o IP");
        }
        String[] parts = text.strip().split("/", -1);
        InetAddress address = parseAddress(parts[0]);
        int bits = address.getAddress().length * 8;
        if (parts.length > 2) {
            throw new InvalidValueException("IP inválido: " + text.strip());
        }
        if (parts.length == 2) {
            try {
                bits = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new InvalidValueException("IP inválido: " + text.strip());
            }
        }
        return new IpRange(address, bits);
    }

    /**
     * A rede de quem está conectado agora ("Usar o IP desta rede"): no IPv4, o IP exato; no IPv6,
     * os primeiros 64 bits, que são os da rede (o fim muda a cada aparelho e com o tempo).
     */
    public static IpRange networkOf(String clientIp) {
        InetAddress address = parseAddress(clientIp);
        return address.getAddress().length == 4 ? new IpRange(address, 32) : new IpRange(address, 64);
    }

    static InetAddress parseAddress(String text) {
        String value = text == null ? "" : text.strip();
        if (!LITERAL.matcher(value).matches()) {
            throw new InvalidValueException("IP inválido: " + value);
        }
        try {
            return InetAddress.getByName(value); // literal: o Java só converte, sem consultar DNS
        } catch (UnknownHostException e) {
            throw new InvalidValueException("IP inválido: " + value);
        }
    }

    /** O IP está dentro da faixa? Compara os primeiros prefixBits bits, um a um. IPv4 e IPv6 não se misturam. */
    public boolean contains(String ip) {
        byte[] candidate;
        try {
            candidate = parseAddress(ip).getAddress();
        } catch (InvalidValueException e) {
            return false;
        }
        byte[] network = address.getAddress();
        if (candidate.length != network.length) {
            return false;
        }
        int fullBytes = prefixBits / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (candidate[i] != network[i]) {
                return false;
            }
        }
        int restBits = prefixBits % 8;
        if (restBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - restBits) & 0xFF;
        return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
    }

    /** Sempre com o tamanho ("189.45.212.18/32"): é assim que vai para o banco e para a tela. */
    @Override
    public String toString() {
        return address.getHostAddress() + "/" + prefixBits;
    }
}
