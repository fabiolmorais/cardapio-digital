package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MediaOwner;
import br.com.cardapio.domain.menu.model.VideoFile;

public interface StartVideoUploadUseCase {

    /**
     * Confere o dono (produto ou opção), os limites e o arquivo, cria o vídeo no serviço de vídeo e a mídia (em
     * conversão, no fim da galeria), e devolve como o navegador envia o arquivo.
     */
    VideoUpload start(EstablishmentId establishmentId, MediaOwner owner, VideoFile file);
}
