package br.com.cardapio.domain.shared.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    @Test
    void acceptsValidCpfWithOrWithoutPunctuation() {
        assertThat(new Document("529.982.247-25").value()).isEqualTo("52998224725");
        assertThat(new Document("52998224725").isCompany()).isFalse();
    }

    @Test
    void acceptsValidNumericCnpj() {
        Document cnpj = new Document("11.222.333/0001-81");
        assertThat(cnpj.value()).isEqualTo("11222333000181");
        assertThat(cnpj.isCompany()).isTrue();
    }

    @Test
    void acceptsAlphanumericCnpjFromJuly2026() {
        // Exemplo oficial da Receita Federal para o CNPJ alfanumérico.
        assertThat(new Document("12.abc.345/01de-35").value()).isEqualTo("12ABC34501DE35");
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "111.111.111-11", "11.222.333/0001-82", "12ABC34501DE36", "123", ""})
    void rejectsInvalidDocuments(String value) {
        assertThatThrownBy(() -> new Document(value)).isInstanceOf(InvalidValueException.class);
    }
}