package br.com.cardapio.application.port.in.device;

import java.time.Instant;

/**
 * O convite pronto para o painel mostrar: o aparelho criado e o QR Code que o vincula. O endereço
 * leva o segredo do convite (depois do #, que o navegador não manda a servidor nenhum); por isso
 * nem ele nem o QR aparecem no toString.
 *
 * @param qrSvg o QR Code do endereço, em SVG
 */
public record DeviceInvite(DeviceSummary device, String url, String qrSvg, Instant expiresAt) {

    @Override
    public String toString() {
        return "DeviceInvite[device=" + device.id() + ", expiresAt=" + expiresAt + "]";
    }
}
