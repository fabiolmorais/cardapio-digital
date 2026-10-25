package br.com.cardapio.domain.account.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugTest {

    @Test
    void createsSlugWithoutAccentsOrSymbols() {
        assertThat(Slug.from("Sakura Sushi — Liberdade").value()).isEqualTo("sakura-sushi-liberdade");
        assertThat(Slug.from("  Bar do Zé & Cia!! ").value()).isEqualTo("bar-do-ze-cia");
    }

    @Test
    void addsSuffixForRepeatedNames() {
        assertThat(Slug.from("Sakura Sushi").withSuffix(2).value()).isEqualTo("sakura-sushi-2");
    }

    @Test
    void rejectsNameWithoutLettersOrNumbers() {
        assertThatThrownBy(() -> Slug.from("!!!")).isInstanceOf(InvalidValueException.class);
    }
}