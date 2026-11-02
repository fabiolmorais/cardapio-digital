package br.com.cardapio.application.port.in.brand;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.LogoFile;

public interface StartLogoUploadUseCase {

    /** Passo 1: confere o arquivo e devolve a URL assinada. */
    LogoUpload start(EstablishmentId establishmentId, LogoFile file);
}
