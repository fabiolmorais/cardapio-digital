package br.com.cardapio.domain.shared.exception;

/** 422: o arquivo (foto ou logo) não chegou ao armazenamento: o envio falhou ou a URL venceu. */
public class UploadNotFoundException extends DomainException {

    public UploadNotFoundException() {
        super("O arquivo não chegou. Envie de novo.");
    }
}
