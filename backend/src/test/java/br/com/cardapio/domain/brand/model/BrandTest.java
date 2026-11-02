package br.com.cardapio.domain.brand.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandTest {

    private final EstablishmentId sakura = EstablishmentId.newId();

    private Brand brand() {
        return new Brand(sakura, "Sakura Sushi", BrandColor.TOMATO, Theme.DARK, null, null, null);
    }

    @Test
    void instagramAcceptsTheHandleTheAtOrTheLink() {
        assertThat(InstagramHandle.parse("@sakurasushi.sp").value()).isEqualTo("sakurasushi.sp");
        assertThat(InstagramHandle.parse("sakura_sushi").value()).isEqualTo("sakura_sushi");
        assertThat(InstagramHandle.parse("https://www.instagram.com/sakurasushi.sp/").value())
                .isEqualTo("sakurasushi.sp");
        assertThat(InstagramHandle.parse("  ")).isNull();
        assertThatThrownBy(() -> InstagramHandle.parse("sakura sushi")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> InstagramHandle.parse("@" + "x".repeat(31))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theReviewLinkMustBeFromGoogle() {
        assertThat(ReviewLink.parse("https://g.page/r/CbX0exemplo/review").value())
                .isEqualTo("https://g.page/r/CbX0exemplo/review");
        assertThat(new ReviewLink("https://search.google.com/local/writereview?placeid=ChIJ").value()).isNotBlank();
        assertThat(ReviewLink.parse("")).isNull();

        // Outro site, http sem "s", um "google" falso no meio do endereço, ou usuário escondido antes do @.
        assertThatThrownBy(() -> ReviewLink.parse("https://golpe.com/avalie")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> ReviewLink.parse("http://g.page/r/abc")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> ReviewLink.parse("https://g.page.golpe.com/r")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> ReviewLink.parse("https://g.page@golpe.com/r")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> ReviewLink.parse("javascript:alert(1)")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void editKeepsTheLogoAndANewLogoGoesToANewFolder() {
        Brand withLogo = brand().withNewLogo();
        String folder = withLogo.logoPath();

        Brand edited = withLogo.edit(" Sakura ", BrandColor.PETROL, Theme.LIGHT, null, null);

        assertThat(edited.name()).isEqualTo("Sakura");
        assertThat(edited.logoPath()).isEqualTo(folder);
        assertThat(folder).startsWith("establishments/" + sakura.value() + "/logo/");
        assertThat(edited.logoFile(LogoVariant.SMALL)).isEqualTo(folder + "/logo-128.png");
        assertThat(edited.withNewLogo().logoPath()).isNotEqualTo(folder);
        assertThat(edited.withoutLogo().hasLogo()).isFalse();
    }

    @Test
    void theNameIsRequired() {
        assertThatThrownBy(() -> brand().edit(" ", BrandColor.TOMATO, Theme.DARK, null, null))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> brand().edit("x".repeat(121), BrandColor.TOMATO, Theme.DARK, null, null))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theLogoFileIsPngOrJpgUpTo2Megabytes() {
        assertThat(new LogoFile("image/png", 2L * 1024 * 1024).sizeBytes()).isEqualTo(2L * 1024 * 1024);
        assertThatThrownBy(() -> new LogoFile("image/svg+xml", 1000))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("Envie a logo em PNG ou JPG");
        assertThatThrownBy(() -> new LogoFile("image/png", 2L * 1024 * 1024 + 1))
                .isInstanceOf(InvalidValueException.class);
    }
}
