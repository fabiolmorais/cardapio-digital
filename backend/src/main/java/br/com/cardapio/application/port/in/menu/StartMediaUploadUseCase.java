package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.PhotoFile;
import br.com.cardapio.domain.menu.model.MediaOwner;

public interface StartMediaUploadUseCase {

    /** Primeiro passo do envio: confere o dono (produto ou opção) e o arquivo e devolve a URL assinada. */
    MediaUpload start(EstablishmentId establishmentId, MediaOwner owner, PhotoFile file);
}
