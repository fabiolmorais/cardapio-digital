package br.com.cardapio.application.port.in.floor;

public interface ViewPublicTableUseCase {

    /** A mesa do QR Code; TableNotFoundException se o token for inventado, trocado ou a mesa estiver desativada. */
    PublicTable view(String token);
}
