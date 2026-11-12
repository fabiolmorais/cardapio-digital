package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.application.port.out.media.ProcessedLogo;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.BannerFile;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.shared.exception.InvalidImageException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.support.InMemoryRodizios;
import br.com.cardapio.support.InMemoryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O banner do rodízio: o envio em dois passos, a troca (apagando o velho) e a volta para a imagem automática. */
class RodizioBannerServicesTest {

    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final InMemoryStorage storage = new InMemoryStorage();
    /** Aceita qualquer arquivo que comece com "BANNER"; o recorte de verdade tem o próprio teste. */
    private final ImageProcessorPort images = new ImageProcessorPort() {
        @Override
        public ProcessedImage process(byte[] original) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedLogo processLogo(byte[] original) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] processBanner(byte[] original) {
            if (!new String(original).startsWith("BANNER")) {
                throw new InvalidImageException("O arquivo não é uma foto JPG ou PNG");
            }
            return "1200x600".getBytes();
        }
    };
    private final RodizioBannerService banners = new RodizioBannerService(rodizios, storage, images);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private Rodizio japones;

    @BeforeEach
    void aRodizio() {
        japones = Rodizio.create(sakura, "Rodízio Japonês", RodizioType.A_LA_MINUTE, "", RodizioRules.NONE,
                List.of(new PriceTier("Adulto", Money.ofCents(129_90), List.of())), Set.of(),
                Instant.parse("2026-11-12T20:00:00Z"));
        rodizios.save(japones);
    }

    private UUID upload(String content) {
        var upload = banners.start(sakura, japones.id(), new BannerFile("image/jpeg", 1_000));
        storage.browserUploads(upload.url(), content.getBytes());
        return upload.uploadId();
    }

    @Test
    void theBannerIsCroppedStoredAndTheOldOneGoes() {
        assertThat(banners.bannerUrl(japones)).isNull();

        Rodizio first = banners.set(sakura, japones.id(), upload("BANNER-1"));
        String firstPath = first.banner();
        assertThat(storage.files.get(firstPath)).asString().isEqualTo("1200x600");
        assertThat(banners.bannerUrl(first)).isEqualTo("https://fotos.test/" + firstPath);
        // O original enviado não fica guardado.
        assertThat(storage.files.keySet()).noneMatch(path -> path.startsWith("uploads/"));

        Rodizio second = banners.set(sakura, japones.id(), upload("BANNER-2"));
        assertThat(second.banner()).isNotEqualTo(firstPath);
        assertThat(storage.files).doesNotContainKey(firstPath);

        Rodizio removed = banners.remove(sakura, japones.id());
        assertThat(removed.banner()).isNull();
        assertThat(storage.files).doesNotContainKey(second.banner());
    }

    @Test
    void notAnImageOrNotUploadedOrTooBigIsRefused() {
        assertThatThrownBy(() -> banners.set(sakura, japones.id(), upload("EXECUTAVEL")))
                .isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> banners.set(sakura, japones.id(), UUID.randomUUID()))
                .isInstanceOf(UploadNotFoundException.class);
        assertThatThrownBy(() -> new BannerFile("image/gif", 1_000)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new BannerFile("image/jpeg", 6L * 1024 * 1024))
                .isInstanceOf(InvalidValueException.class);
        assertThat(rodizios.find(sakura, japones.id()).orElseThrow().banner()).isNull();
    }

    @Test
    void anotherRestaurantDoesNotTouchTheBanner() {
        EstablishmentId other = EstablishmentId.newId();

        assertThatThrownBy(() -> banners.start(other, japones.id(), new BannerFile("image/jpeg", 1_000)))
                .isInstanceOf(RodizioNotFoundException.class);
        assertThatThrownBy(() -> banners.set(other, japones.id(), UUID.randomUUID()))
                .isInstanceOf(RodizioNotFoundException.class);
        assertThatThrownBy(() -> banners.remove(other, japones.id())).isInstanceOf(RodizioNotFoundException.class);
    }
}
