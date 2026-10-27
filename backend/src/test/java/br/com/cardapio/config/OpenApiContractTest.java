package br.com.cardapio.config;

import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O front gera os tipos a partir do OpenAPI (npm run gen:api). Se uma operação não declarar
 * a resposta de sucesso, o tipo gerado fica sem corpo e o front não consegue usá-la.
 * Isso acontece, por exemplo, quando um @ApiResponse na classe do controller substitui o 200
 * que o springdoc criaria sozinho.
 */
@IntegrationTest
class OpenApiContractTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void everyOperationDocumentsItsSuccessResponse() throws Exception {
        String docs = mvc.get().uri("/v3/api-docs").exchange().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(docs, "$.paths");

        List<String> withoutSuccess = new ArrayList<>();
        paths.forEach((path, operations) -> operations.forEach((method, operation) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
            if (responses == null || responses.keySet().stream().noneMatch(code -> code.startsWith("2"))) {
                withoutSuccess.add(method.toUpperCase() + " " + path);
            }
        }));

        assertThat(withoutSuccess).as("operações sem resposta 2xx no OpenAPI").isEmpty();
    }
}
