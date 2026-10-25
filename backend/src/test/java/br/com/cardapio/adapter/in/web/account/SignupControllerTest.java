package br.com.cardapio.adapter.in.web.account;

import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O cadastro é público e cria o próprio restaurante: não há "outro restaurante" para testar aqui.
 * O teste de acesso entre restaurantes aparece nos endpoints autenticados (equipe, mesas…).
 */
@IntegrationTest
class SignupControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    /** E-mail único por teste: o banco do Testcontainers é compartilhado entre as classes de teste. */
    private static String uniqueEmail() {
        return "dono-" + UUID.randomUUID() + "@teste.com";
    }

    private static String body(String email, String establishmentName, String document) {
        return """
                {"establishmentName":"%s","ownerName":"Marina Tanaka","document":"%s",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"sushi2026bom","acceptedTerms":true}
                """.formatted(establishmentName, document, email);
    }

    @Test
    void createsAccountAndReturns201() {
        String email = uniqueEmail();
        var result = mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "Restaurante " + UUID.randomUUID(), "11.222.333/0001-81")).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED).headers().containsHeader("Location");
        assertThat(result).bodyJson().extractingPath("$.establishmentId").isNotNull();
        String hash = jdbc.queryForObject("select password_hash from app_user where email = ?", String.class, email);
        assertThat(hash).isNotEqualTo("sushi2026bom").startsWith("$2");
        String role = jdbc.queryForObject("""
                select m.role from membership m join app_user u on u.id = m.user_id where u.email = ?
                """, String.class, email);
        assertThat(role).isEqualTo("OWNER");
    }

    @Test
    void rejectsDuplicateEmailIgnoringCaseWith409() {
        String email = uniqueEmail();
        mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON)
                .content(body(email, "Primeiro " + UUID.randomUUID(), "11.222.333/0001-81")).exchange();

        var again = mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON)
                .content(body(email.toUpperCase(), "Segundo " + UUID.randomUUID(), "11.222.333/0001-81")).exchange();

        assertThat(again).hasStatus(HttpStatus.CONFLICT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void rejectsInvalidDocumentWith400AndMessage() {
        var result = mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON)
                .content(body(uniqueEmail(), "Restaurante " + UUID.randomUUID(), "111.111.111-11")).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("CPF ou CNPJ inválido");
    }

    @Test
    void rejectsMissingFieldsWith400ListingFieldNamesOnly() {
        var result = mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@y.com\",\"password\":\"curta\",\"acceptedTerms\":false}").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.fields").asArray()
                .contains("establishmentName", "ownerName", "document", "whatsapp", "password", "acceptedTerms");
    }
}