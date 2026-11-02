package br.com.cardapio.application.service.brand;

import br.com.cardapio.application.port.in.brand.BrandSummary;
import br.com.cardapio.application.port.in.brand.LogoUpload;
import br.com.cardapio.application.port.in.brand.UpdateBrandCommand;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.application.port.out.media.ProcessedLogo;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.LogoFile;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;
import br.com.cardapio.domain.shared.exception.InvalidImageException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.exception.UploadNotFoundException;
import br.com.cardapio.support.InMemoryStorage;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandServicesTest {

    /** As marcas em memória, uma por restaurante, todas começando como o cadastro deixa. */
    private final Map<EstablishmentId, Brand> brands = new HashMap<>();
    private final BrandPort brandPort = new BrandPort() {
        @Override
        public Brand find(EstablishmentId id) {
            return brands.computeIfAbsent(id,
                    key -> new Brand(key, "Restaurante", BrandColor.TOMATO, Theme.DARK, null, null, null));
        }

        @Override
        public void save(Brand brand) {
            brands.put(brand.establishmentId(), brand);
        }
    };
    /** Aceita qualquer arquivo que comece com "LOGO"; o processamento de verdade tem o próprio teste. */
    private final ImageProcessorPort images = new ImageProcessorPort() {
        @Override
        public ProcessedImage process(byte[] original) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessedLogo processLogo(byte[] original) {
            if (!new String(original).startsWith("LOGO")) {
                throw new InvalidImageException("O arquivo não é uma foto JPG ou PNG");
            }
            return new ProcessedLogo(Map.of(LogoVariant.SMALL, "128".getBytes(), LogoVariant.LARGE, "512".getBytes()));
        }
    };
    private final InMemoryStorage storage = new InMemoryStorage();
    private final GetBrandService get = new GetBrandService(brandPort, storage);
    private final UpdateBrandService update = new UpdateBrandService(brandPort, storage);
    private final StartLogoUploadService start = new StartLogoUploadService(storage);
    private final SetLogoService set = new SetLogoService(brandPort, storage, images);
    private final RemoveLogoService remove = new RemoveLogoService(brandPort, storage);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final LogoFile png = new LogoFile("image/png", 30_000);

    private BrandSummary uploadLogo(EstablishmentId establishment, String content) {
        LogoUpload upload = start.start(establishment, png);
        storage.browserUploads(upload.url(), content.getBytes());
        return set.set(establishment, upload.uploadId());
    }

    @Test
    void aNewRestaurantStartsWithTheDefaultsAndNoLogo() {
        BrandSummary brand = get.get(sakura);

        assertThat(brand.color()).isEqualTo(BrandColor.TOMATO);
        assertThat(brand.theme()).isEqualTo(Theme.DARK);
        assertThat(brand.logoUrls()).isEmpty();
    }

    @Test
    void theFormCleansTheInstagramAndEmptyFieldsErase() {
        update.update(new UpdateBrandCommand(sakura, "Sakura Sushi", BrandColor.PETROL, Theme.LIGHT,
                "https://instagram.com/sakurasushi.sp", "https://g.page/r/abc/review"));

        BrandSummary saved = update.update(new UpdateBrandCommand(sakura, "Sakura Sushi", BrandColor.PETROL,
                Theme.LIGHT, "@sakurasushi.sp", ""));

        assertThat(saved.instagram()).isEqualTo("sakurasushi.sp");
        assertThat(saved.reviewLink()).isNull();
        assertThat(get.get(sakura).color()).isEqualTo(BrandColor.PETROL);
        assertThatThrownBy(() -> update.update(new UpdateBrandCommand(sakura, "Sakura", BrandColor.PETROL,
                Theme.LIGHT, null, "https://golpe.com"))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theLogoGetsTwoSizesAndTheOriginalIsDropped() {
        BrandSummary brand = uploadLogo(sakura, "LOGO da Sakura");

        String folder = brands.get(sakura).logoPath();
        assertThat(storage.files).containsOnlyKeys(folder + "/logo-128.png", folder + "/logo-512.png");
        assertThat(brand.logoUrls()).containsEntry(LogoVariant.SMALL, "https://fotos.test/" + folder + "/logo-128.png");

        assertThat(remove.remove(sakura).logoUrls()).isEmpty();
        assertThat(get.get(sakura).logoUrls()).isEmpty();
    }

    @Test
    void aLogoThatNeverArrivedIsNotAnImageOrBelongsToAnotherRestaurantIsRefused() {
        LogoUpload neverSent = start.start(sakura, png);
        assertThatThrownBy(() -> set.set(sakura, neverSent.uploadId())).isInstanceOf(UploadNotFoundException.class);

        assertThatThrownBy(() -> uploadLogo(sakura, "<svg onload=alert(1)>")).isInstanceOf(InvalidImageException.class);

        // O uploadId da Sakura, usado por outro restaurante, aponta para a pasta dele: não há nada lá.
        LogoUpload sakurasUpload = start.start(sakura, png);
        storage.browserUploads(sakurasUpload.url(), "LOGO".getBytes());
        assertThatThrownBy(() -> set.set(EstablishmentId.newId(), sakurasUpload.uploadId()))
                .isInstanceOf(UploadNotFoundException.class);
        assertThatThrownBy(() -> set.set(sakura, UUID.randomUUID())).isInstanceOf(UploadNotFoundException.class);
        assertThat(get.get(sakura).logoUrls()).isEmpty();
    }
}
