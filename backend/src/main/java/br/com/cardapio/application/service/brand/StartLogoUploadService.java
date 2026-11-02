package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.LogoUpload;
import br.com.cardapio.application.port.in.brand.StartLogoUploadUseCase;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.LogoFile;

import java.time.Duration;
import java.util.UUID;

/** Sem @Transactional: este passo não lê nem grava nada no banco. */
public class StartLogoUploadService implements StartLogoUploadUseCase {

    static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(5);

    private final MediaStoragePort storage;

    public StartLogoUploadService(MediaStoragePort storage) {
        this.storage = storage;
    }

    /** Como nas fotos: o caminho é montado com o restaurante do token, nos dois passos. */
    static String uploadPath(EstablishmentId establishmentId, UUID uploadId) {
        return "uploads/" + establishmentId.value() + "/logo/" + uploadId;
    }

    @Override
    public LogoUpload start(EstablishmentId establishmentId, LogoFile file) {
        UUID uploadId = UUID.randomUUID();
        var signed = storage.signUpload(uploadPath(establishmentId, uploadId), file.contentType(), file.sizeBytes(),
                UPLOAD_VALIDITY);
        return new LogoUpload(uploadId, signed.url(), signed.headers(), signed.expiresAt());
    }
}
