package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.RodizioBannerUpload;
import br.com.cardapio.application.port.in.rodizio.RodizioBannerUseCase;
import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.BannerFile;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/** A mesma sequência da logo e das fotos: ler, conferir, gravar o arquivo, gravar o banco, apagar o original. */
public class RodizioBannerService implements RodizioBannerUseCase {

    static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(5);

    private final RodizioPort rodizios;
    private final MediaStoragePort storage;
    private final ImageProcessorPort images;

    public RodizioBannerService(RodizioPort rodizios, MediaStoragePort storage, ImageProcessorPort images) {
        this.rodizios = rodizios;
        this.storage = storage;
        this.images = images;
    }

    /** O caminho do envio, montado com o restaurante do token nos dois passos (nunca vindo do navegador). */
    static String uploadPath(EstablishmentId establishmentId, RodizioId rodizioId, UUID uploadId) {
        return "uploads/" + establishmentId.value() + "/rodizio-banner/" + rodizioId.value() + "/" + uploadId;
    }

    @Override
    @Transactional(readOnly = true)
    public RodizioBannerUpload start(EstablishmentId establishmentId, RodizioId rodizioId, BannerFile file) {
        require(establishmentId, rodizioId);
        UUID uploadId = UUID.randomUUID();
        var signed = storage.signUpload(uploadPath(establishmentId, rodizioId, uploadId), file.contentType(),
                file.sizeBytes(), UPLOAD_VALIDITY);
        return new RodizioBannerUpload(uploadId, signed.url(), signed.headers(), signed.expiresAt());
    }

    @Override
    @Transactional
    public Rodizio set(EstablishmentId establishmentId, RodizioId rodizioId, UUID uploadId) {
        Rodizio rodizio = require(establishmentId, rodizioId);
        String uploadPath = uploadPath(establishmentId, rodizioId, uploadId);
        byte[] original = storage.read(uploadPath, BannerFile.MAX_BYTES).orElseThrow(UploadNotFoundException::new);
        byte[] banner = images.processBanner(original);
        String path = rodizio.newBannerPath();
        storage.store(path, banner, "image/jpeg");
        Rodizio changed = rodizio.withBanner(path);
        rodizios.save(changed);
        storage.delete(uploadPath);
        if (rodizio.banner() != null) {
            storage.delete(rodizio.banner());
        }
        return changed;
    }

    @Override
    @Transactional
    public Rodizio remove(EstablishmentId establishmentId, RodizioId rodizioId) {
        Rodizio rodizio = require(establishmentId, rodizioId);
        if (rodizio.banner() == null) {
            return rodizio;
        }
        Rodizio changed = rodizio.withBanner(null);
        rodizios.save(changed);
        storage.delete(rodizio.banner());
        return changed;
    }

    @Override
    public String bannerUrl(Rodizio rodizio) {
        return rodizio.banner() == null ? null : storage.publicUrl(rodizio.banner());
    }

    private Rodizio require(EstablishmentId establishmentId, RodizioId rodizioId) {
        return rodizios.find(establishmentId, rodizioId).orElseThrow(RodizioNotFoundException::new);
    }
}
